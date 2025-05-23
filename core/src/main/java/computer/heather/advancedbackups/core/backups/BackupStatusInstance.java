package computer.heather.advancedbackups.core.backups;

public class BackupStatusInstance {

    private static BackupStatusInstance instance = null;

    private State state = State.INVALID;

    private int progress = -1;
    private int max = -1;

    private long age;


    public static synchronized void setInstance(BackupStatusInstance instance) {
        BackupStatusInstance.instance = instance;
    }

    public static synchronized BackupStatusInstance getInstanceCopy() {
        return instance == null ? null : copyInstance(instance);
    }

    private static synchronized BackupStatusInstance copyInstance(BackupStatusInstance in) {
        BackupStatusInstance other = new BackupStatusInstance();
        other.setMax(in.getMax());
        other.setProgress(in.getProgress());
        other.setState(in.getState());
        other.setAge(in.getAge());
        return other;

    }

    public State getState() {
        return this.state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public int getProgress() {
        return this.progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getMax() {
        return this.max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public long getAge() {
        return this.age;
    }

    public void setAge(long age) {
        this.age = age;
    }


    public static enum State {

        STARTING,
        STARTED,
        COMPLETE,
        FAILED,
        CANCELLED,
        //This one's used for a default, and should never be encountered!
        INVALID;

    }


}