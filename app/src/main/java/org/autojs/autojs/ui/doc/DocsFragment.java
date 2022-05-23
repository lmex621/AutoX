package org.autojs.autojs.ui.doc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.stardust.util.BackPressedHandler;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import org.autojs.autojs.Pref;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.autojs.autojs.R;
import org.autojs.autojs.tool.SimpleObserver;
import org.autojs.autojs.ui.main.FloatingActionMenu;
import org.autojs.autojs.ui.main.FloatingActionMenu;
import org.autojs.autojs.ui.main.QueryEvent;
import org.autojs.autojs.ui.main.ViewPagerFragment;
import org.autojs.autojs.ui.widget.CallbackBundle;
import org.autojs.autojs.ui.widget.EWebView;
import io.reactivex.android.schedulers.AndroidSchedulers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EFragment(R.layout.fragment_online_docs)
public class DocsFragment extends ViewPagerFragment implements BackPressedHandler, FloatingActionMenu.OnFloatingActionButtonClickListener {

    public static final String ARGUMENT_URL = "url";

    @ViewById(R.id.eweb_view_docs)
    EWebView mEWebView;
    com.tencent.smtt.sdk.WebView mWebView;

    private String mIndexUrl = "file:///android_asset/docs/index.html";
    private String mPreviousQuery;
    static private Dialog mDialog;
    public static String tag = "OpenFileDialog";
    static public String sRoot = Environment.getExternalStorageDirectory().getAbsolutePath();
    static final public String sParent = "..";
    static final public String sFolder = ".";
    static final public String sEmpty = "";
    static final private String sOnErrorMsg = "No rights to access!";
    private FloatingActionMenu mFloatingActionMenu;


    public DocsFragment() {
        super(ROTATION_GONE);
        setArguments(new Bundle());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        mIndexUrl = getArguments().getString(ARGUMENT_URL, Pref.getDocumentationUrl() + "index.html");
    }

    @AfterViews
    void setUpViews() {
        mWebView = mEWebView.getWebView();
        Bundle savedWebViewState = getArguments().getBundle("savedWebViewState");
        if (savedWebViewState != null) {
            mWebView.restoreState(savedWebViewState);
        } else {
            mWebView.loadUrl(mIndexUrl);
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        Bundle savedWebViewState = new Bundle();
        mWebView.saveState(savedWebViewState);
        getArguments().putBundle("savedWebViewState", savedWebViewState);
    }

    @Override
    public boolean onBackPressed(Activity activity) {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return false;
    }

    @Override
    protected void onFabClick(FloatingActionButton fab) {
        initFloatingActionMenuIfNeeded(fab);
        if (mFloatingActionMenu.isExpanded()) {
            mFloatingActionMenu.collapse();
        } else {
            mFloatingActionMenu.expand();

        }
    }

    private void initFloatingActionMenuIfNeeded(final FloatingActionButton fab) {
        if (mFloatingActionMenu != null)
            return;
        mFloatingActionMenu = getActivity().findViewById(R.id.floating_action_menu);
        mFloatingActionMenu.getState()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<Boolean>() {
                    @Override
                    public void onNext(@io.reactivex.annotations.NonNull Boolean expanding) {
                        fab.animate()
                                .rotation(expanding ? 45 : 0)
                                .setDuration(300)
                                .start();
                    }
                });
        mFloatingActionMenu.setOnFloatingActionButtonClickListener(this);
    }


    @Subscribe
    public void onQuerySummit(QueryEvent event) {
        if (!isShown()) {
            return;
        }
        if (event == QueryEvent.CLEAR) {
            mWebView.clearMatches();
            mPreviousQuery = null;
            return;
        }
        if (event.isFindForward()) {
            mWebView.findNext(false);
            return;
        }
        if (event.getQuery().equals(mPreviousQuery)) {
            mWebView.findNext(true);
            return;
        }
        mWebView.findAllAsync(event.getQuery());
        mPreviousQuery = event.getQuery();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onClick(FloatingActionButton button, int pos) {
        switch (pos) {
            case 0:
                mWebView.loadUrl(mIndexUrl);
                break;
            case 1:
                HashMap<String, Integer> images = new HashMap<String, Integer>();
                // 下面几句设置各文件类型的图标， 需要你先把图标添加到资源文件夹
                images.put(sRoot, R.drawable.filedialog_root);    // 根目录图标
                images.put(sParent, R.drawable.filedialog_folder_up);    //返回上一层的图标
                images.put(sFolder, R.drawable.filedialog_folder);    //文件夹图标
                images.put(sEmpty, R.drawable.filedialog_file);
                mDialog = createDialog(getActivity(), "打开本地文档", new CallbackBundle() {
                            @Override
                            public void callback(Bundle bundle) {
                                String filepath = bundle.getString("filepath").toLowerCase();
                                if (filepath.endsWith(".html") || filepath.endsWith(".htm") || filepath.endsWith(
                                        ".xhtml"
                                ) || filepath.endsWith(
                                        ".xml"
                                ) || filepath.endsWith(
                                        ".mhtml"
                                ) || filepath.endsWith(
                                        ".mht"
                                ) || filepath.endsWith(
                                        ".txt"
                                ) || filepath.endsWith(
                                        ".js"
                                ) || filepath.endsWith(
                                        ".log"
                                )
                                ) {
                                    mWebView.loadUrl("file://" + filepath);
                                } else {
                                    HashMap<String, String> extraParams = new HashMap<String, String>(); //define empty hashmap
                                    extraParams.put("style", "1");
                                    extraParams.put("local", "true");
                                    com.tencent.smtt.sdk.QbSdk.openFileReader(
                                            getActivity(),
                                            filepath,
                                            extraParams,
                                            new com.tencent.smtt.sdk.ValueCallback<String>() {
                                                @Override
                                                public void onReceiveValue(String it) {
                                                    Log.e("TAG", "OpenFile Callback: $it");
                                                    if ("openFileReader open in QB" == it
                                                            || "filepath error" == it
                                                            || "TbsReaderDialogClosed" == it
                                                            || "fileReaderClosed" == it
                                                    ) {
                                                        com.tencent.smtt.sdk.QbSdk.closeFileReader(
                                                                getActivity()
                                                        );
                                                    }
                                                }
                                            });
                                }
                            }
                        },
                        "html;htm;xhtml;xml;mhtml;doc;docx;ppt;pptx;xls;xlsx;pdf;txt;epub",
                        images);
                mDialog.show();
                break;
            default:
                break;
        }

    }



    // 参数说明
    // context:上下文
    // dialogId:对话框ID
    // title:对话框标题
    // callback:一个传递Bundle参数的回调接口
    // suffix:需要选择的文件后缀，比如需要选择wav、mp3文件的时候设置为".wav;.mp3;"，注意最后需要一个分号(;)
    // images:用来根据后缀显示的图标资源ID。
    //	根目录图标的索引为sRoot;
    //	父目录的索引为sParent;
    //	文件夹的索引为sFolder;
    //	默认图标的索引为sEmpty;
    //	其他的直接根据后缀进行索引，比如.wav文件图标的索引为"wav"
    public Dialog createDialog(Context context, String title, CallbackBundle callback, String suffix, Map<String, Integer> images, String rootDir) {
        if (!rootDir.isEmpty() && (new File(rootDir)).isDirectory()) {
            sRoot = rootDir;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(new FileSelectView(context, callback, suffix, images));
        mDialog = builder.create();
        mDialog.setTitle(title);
        return mDialog;
    }

    public Dialog createDialog(Context context, String title, CallbackBundle callback, String suffix, Map<String, Integer> images) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(new FileSelectView(context, callback, suffix, images));
        mDialog = builder.create();
        //dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setTitle(title);
        return mDialog;
    }

    static class FileSelectView extends ListView implements AdapterView.OnItemClickListener {


        private CallbackBundle callback = null;
        private String path = sRoot;
        private List<Map<String, Object>> list = null;
        private String suffix = null;

        private Map<String, Integer> imageMap = null;

        public FileSelectView(Context context, CallbackBundle callback, String suffix, Map<String, Integer> images) {
            super(context);
            this.imageMap = images;
            this.suffix = suffix == null ? "" : suffix.toLowerCase();
            this.callback = callback;
            this.setOnItemClickListener(this);
            refreshFileList();
        }

        private String getSuffix(String filename) {
            int dix = filename.lastIndexOf('.');
            if (dix < 0) {
                return "";
            } else {
                return filename.substring(dix);
            }
        }

        private int getImageId(String s) {
            if (imageMap == null) {
                return 0;
            } else if (imageMap.containsKey(s)) {
                return imageMap.get(s);
            } else if (imageMap.containsKey(sEmpty)) {
                return imageMap.get(sEmpty);
            } else {
                return 0;
            }
        }

        private int refreshFileList() {
            // 刷新文件列表
            File[] files = null;
            try {
                files = new File(path).listFiles();
            } catch (Exception e) {
                files = null;
            }
            if (files == null) {
                // 访问出错
                Toast.makeText(getContext(), sOnErrorMsg, Toast.LENGTH_SHORT).show();
                return -1;
            }
            if (list != null) {
                list.clear();
            } else {
                list = new ArrayList<Map<String, Object>>(files.length);
            }

            // 用来先保存文件夹和文件夹的两个列表
            ArrayList<Map<String, Object>> lfolders = new ArrayList<Map<String, Object>>();
            ArrayList<Map<String, Object>> lfiles = new ArrayList<Map<String, Object>>();

            if (!this.path.equals(sRoot)) {
                // 添加根目录 和 上一层目录
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("name", sRoot);
                map.put("path", sRoot);
                map.put("img", getImageId(sRoot));
                list.add(map);

                map = new HashMap<String, Object>();
                map.put("name", sParent);
                map.put("path", path);
                map.put("img", getImageId(sParent));
                list.add(map);
            }

            for (File file : files) {
                if (file.isDirectory() && file.listFiles() != null) {
                    // 添加文件夹
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("name", file.getName());
                    map.put("path", file.getPath());
                    map.put("img", getImageId(sFolder));
                    lfolders.add(map);
                } else if (file.isFile()) {
                    // 添加文件
                    String sf = getSuffix(file.getName()).toLowerCase();
//                    Toast.makeText(getContext(), sf, Toast.LENGTH_SHORT).show();
                    String[] suffixList = suffix.split(";");
                    boolean hasSuffix = false;
                    for (String item : suffixList) {
                        if (sf.contains("." + item)) {
                            hasSuffix = true;
                            break;
                        }
                    }
                    if (suffix == null || suffix.length() == 0 || (sf.length() > 0 && hasSuffix)) {
                        Map<String, Object> map = new HashMap<String, Object>();
                        map.put("name", file.getName());
                        map.put("path", file.getPath());
                        map.put("img", getImageId(suffixList[0]));
                        lfiles.add(map);
                    }
                }
            }

            list.addAll(lfolders); // 先添加文件夹，确保文件夹显示在上面
            list.addAll(lfiles);    //再添加文件


            SimpleAdapter adapter = new SimpleAdapter(getContext(), list, R.layout.filedialogitem, new String[]{"img", "name", "path"}, new int[]{R.id.filedialogitem_img, R.id.filedialogitem_name, R.id.filedialogitem_path});
            this.setAdapter(adapter);
            return files.length;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            // 条目选择
            String pt = (String) list.get(position).get("path");
            String fn = (String) list.get(position).get("name");
            if (fn.equals(sRoot) || fn.equals(sParent)) {
                // 如果是更目录或者上一层
                File fl = new File(pt);
                String ppt = fl.getParent();
                if (ppt != null) {
                    // 返回上一层
                    path = ppt;
                } else {
                    // 返回更目录
                    path = sRoot;
                }
            } else {
                File fl = new File(pt);
                if (fl.isFile()) {
                    // 如果是文件
                    mDialog.dismiss(); // 让文件夹对话框消失

                    // 设置回调的返回值
                    Bundle bundle = new Bundle();
                    bundle.putString("filepath", pt);
                    bundle.putString("filename", fn);
                    // 调用事先设置的回调函数
                    this.callback.callback(bundle);
                    return;
                } else if (fl.isDirectory()) {
                    // 如果是文件夹
                    // 那么进入选中的文件夹
                    path = pt;
                }
            }
            this.refreshFileList();
        }
    }

}
