package org.dalol.apkutility.presenter;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.view.View;

import org.dalol.apkutility.model.adapter.AppsAdapter;
import org.dalol.apkutility.model.callback.LifeCycleCallback;
import org.dalol.apkutility.model.exception.ApkDiggerException;
import org.dalol.apkutility.presenter.base.BasePresenter;
import org.dalol.apkutility.presenter.task.GetInstalledAppTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by Filippo-TheAppExpert on 7/27/2015.
 */
public class MainViewPresenter extends BasePresenter implements LifeCycleCallback, GetInstalledAppTask.AppFoundListener {

    private MainViewListener mListener;
    private int mPosition;

    public MainViewPresenter(MainViewListener listener) {
        mListener = listener;
    }

    @Override
    public void initialize() {
    }

    public void loadInstalledApps() {
        if (!mListener.getPackageInfoList().isEmpty()) {
            mListener.getPackageInfoList().clear();
            mListener.getAppsAdapter().notifyDataSetChanged();
        }
        new GetInstalledAppTask(mListener.getMainContext(), MainViewPresenter.this).execute();
    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onStop() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void onActivityCreated(@Nullable Activity activity) {

    }

    @Override
    public void onStart() {
        mListener.onShowDialog();
    }

    @Override
    public void onApp(PackageInfo packageInfo) {
        mListener.onPublishApp(packageInfo);
    }

    @Override
    public void onAppList(List<PackageInfo> packageInfos) {
        mListener.onPublishSortedAppList(packageInfos);
    }

    @Override
    public void onComplete() {
        mListener.onHideDialog();
    }

    public int getPosition() {
        return mPosition;
    }

    public void setPosition(int position) {
        mPosition = position;
    }

    public void showApplicationInfo(int position) {

        PackageInfo currentPackageInfo = mListener.getPackageInfoList().get(position);
        String packageName = currentPackageInfo.packageName;

        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            mListener.startAppIntent(intent);

        } catch (ActivityNotFoundException e) {
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
            mListener.startAppIntent(intent);
        }
    }

    public void shareApp(int position) {

        PackageInfo currentPackageInfo = mListener.getPackageInfoList().get(position);
        File file = new File(currentPackageInfo.applicationInfo.publicSourceDir);
        writeToStorage(file, currentPackageInfo);
    }

    private void writeToStorage(File outputFile, PackageInfo currentPackageInfo) {

        try {
            String file_name = currentPackageInfo.applicationInfo.loadLabel(mListener.getPkgManager()).toString();

            File apkFileCopy = new File(Environment.getExternalStorageDirectory().toString() + "/APK-Digger");
            apkFileCopy.mkdirs();
            apkFileCopy = new File(apkFileCopy.getPath() + "/" + file_name + ".apk");
            apkFileCopy.createNewFile();

            InputStream in = new FileInputStream(outputFile);
            OutputStream out = new FileOutputStream(apkFileCopy);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.putExtra(
                    Intent.EXTRA_SUBJECT, "APK Digger");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "Sharing " + currentPackageInfo.applicationInfo.loadLabel(mListener.getPkgManager()).toString() + " app.");
            apkFileCopy.setReadable(true, false);


            shareIntent.setType("*/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(apkFileCopy));
            shareIntent.addCategory("android.intent.category.DEFAULT");
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            mListener.startAppIntent(Intent.createChooser(shareIntent, "Share " + apkFileCopy.getName()));

        } catch (FileNotFoundException ex) {
            mListener.onException(new ApkDiggerException("FileNotFoundException :: " + ex));
            System.out.println(ex.getMessage() + " in the specified directory.");
        } catch (IOException e) {
            mListener.onException(new ApkDiggerException("IOException :: " + e));
            System.out.println(e.getMessage());
        }
    }

    public void startImplicitApp(String packageName) {
        try {
            mListener.startAppIntent(mListener.getPkgManager().getLaunchIntentForPackage(packageName));
        } catch (Exception ex) {
            mListener.onException(new ApkDiggerException("ApkDiggerException :: " + ex));
        }
    }

    public void openApplication(int position) {
        PackageInfo currentPackageInfo = mListener.getPackageInfoList().get(position);
        try {
            String packageName = currentPackageInfo.packageName;
            mListener.startImplicitApp(packageName);
        } catch (Exception ex) {
            mListener.onException(new ApkDiggerException("ApkDiggerException :: " + ex));
        }
    }

    public void uninstallApp(int position) {
        PackageInfo currentPackageInfo = mListener.getPackageInfoList().get(position);
        String packageName = currentPackageInfo.packageName;

        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
        intent.setData(Uri.parse("package:" + packageName));
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        setPosition(position);
        mListener.startAppIntentForResult(intent);
    }

    public void removeAppFromList(int position) {
        mListener.getPackageInfoList().remove(position);
        mListener.getAppsAdapter().notifyItemRemoved(position);
    }

    public interface MainViewListener {

        Context getMainContext();

        PackageManager getPkgManager();

        void onShowDialog();

        void onPublishApp(PackageInfo packageInfo);

        void onHideDialog();

        void showFilterPopup(View view, int position, boolean isSystem);

        List<PackageInfo> getPackageInfoList();

        AppsAdapter getAppsAdapter();

        void startAppIntent(Intent intent);

        void startAppIntentForResult(Intent intent);

        void onException(ApkDiggerException exception);

        void startImplicitApp(String packageName);

        void onPublishSortedAppList(List<PackageInfo> packageInfos);
    }
}
