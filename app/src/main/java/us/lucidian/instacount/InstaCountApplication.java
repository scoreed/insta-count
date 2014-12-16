package us.lucidian.instacount;

import android.app.Application;

import com.appkilt.client.AppKilt;

@SuppressWarnings("UnusedDeclaration")
public class InstaCountApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppKilt.init(this, "d13fae33-434b-4d3e-8efd-d83f7e040760");
    }
}