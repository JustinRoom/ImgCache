package jsc.org.lib.img.selector.model;

import android.os.Parcel;
import android.os.Parcelable;

public class LocalMedia implements Parcelable {
    public String path;
    public long duration;
    public long lastUpdateAt;
    public int checkStatus = 0;//1checked

    public boolean isChecked() {
        return checkStatus == 1;
    }

    public LocalMedia() {
    }

    public LocalMedia(String path) {
        this.path = path;
    }

    protected LocalMedia(Parcel in) {
        path = in.readString();
        duration = in.readLong();
        lastUpdateAt = in.readLong();
        checkStatus = in.readInt();
    }

    public static final Creator<LocalMedia> CREATOR = new Creator<LocalMedia>() {
        @Override
        public LocalMedia createFromParcel(Parcel in) {
            return new LocalMedia(in);
        }

        @Override
        public LocalMedia[] newArray(int size) {
            return new LocalMedia[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeLong(duration);
        dest.writeLong(lastUpdateAt);
        dest.writeInt(checkStatus);
    }

    public LocalMedia copy() {
        LocalMedia media = new LocalMedia();
        media.path = path;
        media.duration = duration;
        media.lastUpdateAt = lastUpdateAt;
        media.checkStatus = checkStatus;
        return media;
    }
}
