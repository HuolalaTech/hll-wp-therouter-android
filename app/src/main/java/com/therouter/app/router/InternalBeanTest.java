package com.therouter.app.router;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.List;

public class InternalBeanTest implements Serializable, Parcelable {

    public String nowDate;
    public List<RowBean> row;

    public static class RowBean implements Serializable, Parcelable {
        public String hello;

        public String getHello() {
            return hello;
        }

        public void setHello(String hello) {
            this.hello = hello;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.hello);
        }

        public void readFromParcel(Parcel source) {
            this.hello = source.readString();
        }

        public RowBean() {
        }

        protected RowBean(Parcel in) {
            this.hello = in.readString();
        }

        public static final Creator<RowBean> CREATOR = new Creator<RowBean>() {
            @Override
            public RowBean createFromParcel(Parcel source) {
                return new RowBean(source);
            }

            @Override
            public RowBean[] newArray(int size) {
                return new RowBean[size];
            }
        };
    }

    public String getNowDate() {
        return nowDate;
    }

    public void setNowDate(String nowDate) {
        this.nowDate = nowDate;
    }

    public List<RowBean> getRow() {
        return row;
    }

    public void setRow(List<RowBean> row) {
        this.row = row;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.nowDate);
        dest.writeTypedList(this.row);
    }

    public void readFromParcel(Parcel source) {
        this.nowDate = source.readString();
        this.row = source.createTypedArrayList(RowBean.CREATOR);
    }

    public InternalBeanTest() {
    }

    protected InternalBeanTest(Parcel in) {
        this.nowDate = in.readString();
        this.row = in.createTypedArrayList(RowBean.CREATOR);
    }

    public static final Creator<InternalBeanTest> CREATOR = new Creator<InternalBeanTest>() {
        @Override
        public InternalBeanTest createFromParcel(Parcel source) {
            return new InternalBeanTest(source);
        }

        @Override
        public InternalBeanTest[] newArray(int size) {
            return new InternalBeanTest[size];
        }
    };
}
