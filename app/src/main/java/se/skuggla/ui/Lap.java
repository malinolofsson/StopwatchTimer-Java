package se.skuggla.ui;

/**
 * Created by Malin on 2014-01-30.
 */
public class Lap {

    private long mId;
    private String mlapTime;

    public Lap(long mId, String lapTime) {
        this.mId = mId;
        this.mlapTime = lapTime;
    }

    public long getmId() {
        return mId;
    }

    public void setmId(long mId) {
        this.mId = mId;
    }

    public String getLapTime() {
        return mlapTime;
    }

    public void setMlapTime(String mlapTime) {
        this.mlapTime = mlapTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Lap lap = (Lap) o;

        if (mId != lap.mId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (mId ^ (mId >>> 32));
    }
}
