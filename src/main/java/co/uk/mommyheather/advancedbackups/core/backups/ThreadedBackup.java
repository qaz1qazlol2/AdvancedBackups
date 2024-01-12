package co.uk.mommyheather.advancedbackups.core.backups;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import co.uk.mommyheather.advancedbackups.core.ABCore;
import co.uk.mommyheather.advancedbackups.core.backups.gson.BackupManifest;
import co.uk.mommyheather.advancedbackups.core.backups.gson.HashList;
import co.uk.mommyheather.advancedbackups.core.config.ConfigManager;

public class ThreadedBackup extends Thread {
    private static GsonBuilder builder = new GsonBuilder(); 
    private static Gson gson;
    private long delay;
    private static int count;
    private static float partialSize;
    private static float completeSize;
    public static volatile boolean running = false;
    public static volatile boolean wasRunning = false;
    private static String backupName;
    private Consumer<String> output;
    private boolean snapshot = false;
    private boolean shutdown = false;
    
    static {
        builder.setPrettyPrinting();
        gson = builder.create();
    }
    
    public ThreadedBackup(long delay, Consumer<String> output) {
        setName("AB Active Backup Thread");
        this.output = output;
        this.delay = delay;
        count = 0;
        partialSize = 0F;
        completeSize = 0F;
    }

    @Override
    public void run() {
        try {
            sleep(delay);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!shutdown) ABCore.clientContactor.backupStarting();

        try {
            makeBackup();
            if (!shutdown) ABCore.clientContactor.backupComplete();
        } catch (InterruptedException e) {
            output.accept("Backup cancelled!");
            performDelete(new File(ABCore.backupPath));
            if (!shutdown) ABCore.clientContactor.backupCancelled();
        } catch (Exception e) {
            ABCore.errorLogger.accept("ERROR MAKING BACKUP!");
            e.printStackTrace();
            if (!shutdown) ABCore.clientContactor.backupFailed();
            performDelete(new File(ABCore.backupPath));
        }

        BackupWrapper.finishBackup(snapshot);
        output.accept("Backup complete!");
        wasRunning = true;
        running = false;
    }


    public void makeBackup() throws Exception {

        File file = new File(ABCore.backupPath);
        backupName = ABCore.serialiseBackupName("incomplete");

        if (snapshot) {
            makeZipBackup(file, true);
            output.accept("Snapshot created! This will not be auto-deleted.");
            performRename(file);
            return;
        }

        switch(ConfigManager.type.get()) {
            case "zip" : {
                makeZipBackup(file, false);
                break;
            }
            case "differential" : {
                makeDifferentialOrIncrementalBackup(file, true);
                break;
            }
            case "incremental" : {
                makeDifferentialOrIncrementalBackup(file, false);
                break;
            }
        }

        performRename(file);

    }


    private void makeZipBackup(File file, boolean b) throws InterruptedException, IOException {
        try {

            File zip = new File(file.toString() + (snapshot ? "/snapshots/" : "/zips/"), backupName + ".zip");
            if (!ConfigManager.silent.get()) {
                ABCore.infoLogger.accept("Preparing " + (snapshot ? "snapshot" : "zip") + " backup name: " + zip.getName().replace("incomplete", "backup"));
            }
            output.accept("Preparing " + (snapshot ? "snapshot" : "zip") + " backup name: " + zip.getName().replace("incomplete", "backup"));
            FileOutputStream outputStream = new FileOutputStream(zip);
            ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
            zipOutputStream.setLevel((int) ConfigManager.compression.get());

            ArrayList<Path> paths = new ArrayList<>();

            Files.walkFileTree(ABCore.worldDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    Path targetFile;
                    targetFile = ABCore.worldDir.relativize(file);
                    if (targetFile.toFile().getName().compareTo("session.lock") == 0) {
                        return FileVisitResult.CONTINUE;
                    }
                    paths.add(file);

                
                    return FileVisitResult.CONTINUE;
                    }
            });

            Path targetFile;

            int max = paths.size();
            int index = 0;

            if (!shutdown) ABCore.clientContactor.backupProgress(index, max);

            for (Path path : paths) {
                try {
                    targetFile = ABCore.worldDir.relativize(path);
                    zipOutputStream.putNextEntry(new ZipEntry(targetFile.toString()));
                    byte[] bytes = new byte[(int) ConfigManager.buffer.get()];
                    FileInputStream is = new FileInputStream(path.toFile());
                    while (true) {
                        int i = is.read(bytes);
                        if (i < 0) break;
                        zipOutputStream.write(bytes, 0, i);
                    }

                    is.close();
                    
                    zipOutputStream.closeEntry();
                    //We need to handle interrupts in various styles in different parts of the process!
                    if (isInterrupted()) {
                        zipOutputStream.close();
                        //Here, we need to close the outputstream - otherwise we risk a leak!
                        throw new InterruptedException();
                    }
                    index++;
                    if (!shutdown) ABCore.clientContactor.backupProgress(index, max);
                }
                catch (IOException e) {
                    e.printStackTrace();
                    ABCore.errorLogger.accept(file.toString());
                    throw e;
                }
                
            }

            zipOutputStream.flush();
            zipOutputStream.close();

        } catch (IOException e){
            e.printStackTrace();
            throw e;
        }
        
    }
    


    private void makeDifferentialOrIncrementalBackup(File location, boolean differential) throws InterruptedException, IOException {
        try {
            if (!ConfigManager.silent.get()) {
                ABCore.infoLogger.accept("Preparing " + (differential ? "differential" : "incremental") + " backup name: " + backupName.replace("incomplete", "backup"));
            }
            output.accept("Preparing " + (differential ? "differential" : "incremental") + " backup name: " + backupName.replace("incomplete", "backup"));
            long time = 0;


            File manifestFile = new File(location.toString() + "/manifest.json");
            BackupManifest manifest;
            if (manifestFile.exists()) {
                try {
                    manifest = gson.fromJson(new String(Files.readAllBytes(manifestFile.toPath())), BackupManifest.class);

                } catch (JsonParseException e) {

                    ABCore.errorLogger.accept("Malformed backup manifest! It will have to be reset...");
                    output.accept("Malformed backup manifest! It will have to be reset...");
                    e.printStackTrace();

                    manifest = BackupManifest.defaults();
                }
            }
            else {
                manifest = BackupManifest.defaults();
            }

            if (manifest.differential.hashList == null) manifest.differential.hashList = new HashList();
            if (manifest.incremental.hashList == null) manifest.incremental.hashList = new HashList();

            //mappings here - file path and md5 hash
            Map<String, String> comp = differential ? manifest.differential.getHashList().getHashes() : manifest.incremental.getHashList().getHashes();
            Map<String, String> newHashes = new HashMap<String, String>();

            //long comp = differential ? manifest.differential.getLastBackup() : manifest.incremental.getLastBackup();
            ArrayList<Path> toBackup = new ArrayList<>();
            ArrayList<Path> completeBackup = new ArrayList<>();

            int chain = differential ? manifest.differential.chainLength : manifest.incremental.chainLength;


            boolean completeTemp = chain >= ConfigManager.length.get() ? true : false;
            
            Files.walkFileTree(ABCore.worldDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    Path targetFile;
                    targetFile = ABCore.worldDir.relativize(file);
                    if (targetFile.toFile().getName().compareTo("session.lock") == 0) {
                        return FileVisitResult.CONTINUE;
                    }
                    count++;
                    completeSize += attributes.size();
                    String hash = getFileHash(file.toAbsolutePath());
                    String compHash = comp.getOrDefault(targetFile.toString(), "");
                    completeBackup.add(targetFile);
                    if (completeTemp || !compHash.equals(hash)) {
                        toBackup.add(targetFile);
                        partialSize += attributes.size();
                        newHashes.put(targetFile.toString(), hash);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            boolean complete = completeTemp;
            if (toBackup.size() >= count) {
                complete = true;
            }
            if ((partialSize / completeSize) * 100F > ConfigManager.chainsPercent.get()) {
                complete = true;
                toBackup.clear();
                toBackup.addAll(completeBackup);
            }

            if (complete || !differential) {
                comp.putAll(newHashes);
            }
            
            backupName += complete? "-full":"-partial";


            //We need to handle interrupts in various styles in different parts of the process!
            if (isInterrupted()) {
                //Here however, we have nothing to close! just throw
                throw new InterruptedException();
            }

            if (ConfigManager.compressChains.get()) {
                File zip = differential ? new File(location.toString() + "/differential/", backupName +".zip") : new File(location.toString() + "/incremental/", backupName +".zip");
                FileOutputStream outputStream = new FileOutputStream(zip);
                ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
                zipOutputStream.setLevel((int) ConfigManager.compression.get());

                int max = toBackup.size();
                int index = 0;
    
                if (!shutdown) ABCore.clientContactor.backupProgress(index, max);
                for (Path path : toBackup) {
                    zipOutputStream.putNextEntry(new ZipEntry(path.toString()));

                    byte[] bytes = new byte[(int) ConfigManager.buffer.get()];
                    FileInputStream is = new FileInputStream(new File(ABCore.worldDir.toString(), path.toString()));
                    while (true) {
                        int i = is.read(bytes);
                        if (i < 0) break;
                        zipOutputStream.write(bytes, 0, i);
                    }
                    is.close();
                    zipOutputStream.write(bytes, 0, bytes.length);
                    zipOutputStream.closeEntry();
                    index++;
                    if (!shutdown) ABCore.clientContactor.backupProgress(index, max);

                    //We need to handle interrupts in various styles in different parts of the process!
                    if (isInterrupted()) {
                        zipOutputStream.close();
                        //again, we need to close the outputstream - otherwise we risk a leak!
                        throw new InterruptedException();
                    }
                }
                zipOutputStream.flush();
                zipOutputStream.close();

                time = zip.lastModified();
            }
            else {
                File dest = differential ? new File(location.toString() + "/differential/", backupName + "/") :new File(location.toString() + "/incremental/", backupName + "/");
                dest.mkdirs();
                int max = toBackup.size();
                int index = 0;
                
                if (!shutdown) ABCore.clientContactor.backupProgress(index, max);
                for (Path path : toBackup) {
                    File out = new File(dest, path.toString());
                    if (!out.getParentFile().exists()) {
                        out.getParentFile().mkdirs();
                    }
                    Files.copy(new File(ABCore.worldDir.toString(), path.toString()).toPath(), out.toPath());
                    index++;
                    if (!shutdown) ABCore.clientContactor.backupProgress(index, max);
                }
                time = dest.lastModified();
            }

            //Finally, update + write the manifest
            if (complete || toBackup.size() >= count) {
                if (differential) {
                    manifest.differential.setChainLength(0);
                    manifest.differential.setLastBackup(new Date().getTime());
                }
                else {
                    manifest.incremental.setChainLength(0);
                    manifest.incremental.setLastBackup(new Date().getTime());
                }
            }
            else {
                if (differential) {
                    manifest.differential.chainLength++;
                    //manifest.differential.setLastBackup(new Date().getTime());
                }
                else {
                    manifest.incremental.chainLength++;
                    manifest.incremental.setLastBackup(new Date().getTime());
                }
            }

            FileWriter writer = new FileWriter(manifestFile);
            writer.write(gson.toJson(manifest));
            writer.flush();
            writer.close();
    
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }


    }

    public void snapshot() {
        snapshot = true;
    }

    public void shutdown() {
        shutdown = true;
    }



    private String getFileHash(Path path) {
        try {
            
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] data = new byte[(int) ConfigManager.buffer.get()];
            FileInputStream is = new FileInputStream(path.toFile());
            while (true) {
                int i = is.read(data);
                if (i < 0) break;
                md5.update(data, 0, i);

            }
            byte[] hash = md5.digest();
            String checksum = new BigInteger(1, hash).toString(16);
            return checksum;
        } catch (IOException | NoSuchAlgorithmException e) {
            ABCore.errorLogger.accept("ERROR CALCULATING HASH FOR FILE! " + path.getFileName());
            ABCore.errorLogger.accept("It will be backed up anyway.");
            e.printStackTrace();
            return Integer.toString(new Random().nextInt());
        }
    }


    public static void performRename(File location) {
        //Renames all incomplete backups to no longer have the incomplete marker. This is only done after a successful backup!
       System.out.println("REname!"); 
    }
    
    private void performDelete(File file) {
        //Purges all incomplete backups. This is only done after a cancelled or failed backup!
        System.out.println("Delete!"); 
    }


}
