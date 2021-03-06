package com.yagna.petra.app.Product.Product;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.yagna.petra.app.Cropper.FileUtils;
import com.yagna.petra.app.Cropper.ImageCropActivity;
import com.yagna.petra.app.Model.Product.Categories;
import com.yagna.petra.app.Model.Product.Products;
import com.yagna.petra.app.Model.Product.SubCategories;
import com.yagna.petra.app.R;
import com.yagna.petra.app.Util.Constants;
import com.yagna.petra.app.Util.CustomDialog;
import com.yagna.petra.app.Util.PrefUtil;
import com.yagna.petra.app.Util.TouchImageView;
import com.yagna.petra.app.Util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import id.zelory.compressor.Compressor;
import id.zelory.compressor.FileUtil;

public class ProductActivity extends AppCompatActivity {
    public ArrayList<Bitmap> bitmapArray;
    private EditText product_name, p_price, p_description, p_stock;
    private ImageView p_image;
    private static final int CAMERA_REQUEST = 1888;
    private static int REQUEST_PICTURE = 1;
    private static int REQUEST_CROP_PICTURE = 2;
    private File lastCaptured;
    private Uri imageUri;
    public RecyclerView listview;
    int SELECT_FILE=2,REQUEST_CAMERA=3;
    public static ArrayList<Products.Product> product_array=new ArrayList<>();
    public TextView tv_no_record;
    private ProductAdapter adapter;
    private SharedPreferences common_mypref;
    private JSONObject returnedData;
    private Utils util;
    private CustomDialog cd;
    private TextView title,texttitle_product;
    private ProductActivity context;
    public int position;
    private JSONArray returnedImageData;
    public ArrayList<String[]> imageData;
    Uri myuri;
    private Uri imageToUploadUri;
    String imageid=null,productid=null;
    SimpleDateFormat sdf;
    String currentDateandTime;
    boolean imageupdate=false,flag=false;
    JSONArray j_arrayupdatearray;
    String productimagid[]=null;

    TextInputLayout txtxtitlep_discrption,txtxtitlep_name,txtxtitlep_code,txtxtitlep_price;
    TextView txt_producttitle;
    private LinearLayout ly_spinner;
    private Spinner spn_category,spn_subcategory;
    private String admin_id;
    private String merchant_location_id;
    private ArrayList<Categories.Category> categories;
    private ArrayList<String> cate_array;
    private ArrayList<SubCategories.SubCategory> sub_categories;
    private ArrayList<String> sub_cate_array;
    private CardView cv_subcategory;
    private RecyclerView.LayoutManager storiesLayoutManager;
    private EditText p_sku;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setVisibility(View.VISIBLE);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
       // fillBitmapArrayList();
        //fillDataArraylist();
        context=ProductActivity.this;
        common_mypref = getApplicationContext().getSharedPreferences(
                "user", 0);
        util=new Utils(context);
        tv_no_record = (TextView) findViewById(R.id.tv_no_record_);
        util=new Utils(context);
        cd=new CustomDialog(context);
        txt_producttitle=(TextView)findViewById(R.id.txt_branchtitle);
        txt_producttitle.setText("Products");
        tv_no_record = (TextView) findViewById(R.id.tv_no_record_);
        ly_spinner = (LinearLayout) findViewById(R.id.ly_spinner);
        ly_spinner.setVisibility(View.VISIBLE);
        spn_category = (Spinner) findViewById(R.id.spn_category);
        spn_subcategory= (Spinner) findViewById(R.id.spn_subcategory);
        cv_subcategory = (CardView)findViewById(R.id.cv_subcategory);
        cv_subcategory.setVisibility(View.VISIBLE);
        try {
            JSONObject jobj = new JSONObject(PrefUtil.getLoginData(common_mypref));
            admin_id=jobj.getJSONObject("admin").getString("admin_id");
            merchant_location_id =jobj.getJSONObject("admin").getString("merchant_location_id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        bitmapArray=new ArrayList<>();
        product_array = new ArrayList<>();
        imageData = new ArrayList<>();
        listview=(RecyclerView) findViewById(R.id.listview);
        storiesLayoutManager = createLayoutManager(getResources());
        listview.setLayoutManager(storiesLayoutManager);
        fillData(1);
        sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
      /*  listview.setAdapter(adapter = new ActionListAdpter(ProductActivity.this,product_array,imageData));
        listview.setVisibility(View.VISIBLE);
        tv_no_record.setVisibility(View.GONE);*/
    }
    private RecyclerView.LayoutManager createLayoutManager(Resources resources) {
        int spans = resources.getInteger(R.integer.feed_columns);
        return new StaggeredGridLayoutManager(spans, RecyclerView.VERTICAL);
    }

    private void fillData(int i){
        Log.e("called by",""+i);
        setUrlPayload(Constants.GETLIST_1,"");
    }
    public void addData(final boolean flag, final int position){

        final Dialog alertDialog = new Dialog(ProductActivity.this, android.R.style.Theme_Black_NoTitleBar);
        LayoutInflater inflater = getLayoutInflater();
        final View rowView = (View) inflater.inflate(R.layout.dialog_add_product, null);
        Rect displayRectangle = new Rect();
        Window window = getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
        Objects.requireNonNull(alertDialog.getWindow()).setBackgroundDrawableResource(R.color.translucent_black);
        product_name=(EditText) rowView.findViewById(R.id.p_name);
        p_price =(EditText) rowView.findViewById(R.id.p_price);
        p_stock =(EditText) rowView.findViewById(R.id.p_stock);
        p_description =(EditText) rowView.findViewById(R.id.p_discrption);
        p_image = (ImageView) rowView.findViewById(R.id.p_cropimage);
        p_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });
        p_sku = (EditText) rowView.findViewById(R.id.p_sku);


        if(flag) {
            this.flag=true;
            if(product_array.get(position).image_full_path.trim().length()!=0)
            {
                Picasso.get()
                        .load(product_array.get(position).image_full_path.replace(" ","%20"))
                        .placeholder(R.mipmap.petra)
                        .error(R.mipmap.petra)
                        .into(p_image);
            }
            else{
                Picasso.get()
                        .load(R.mipmap.petra)
                        .placeholder(R.mipmap.petra)
                        .error(R.mipmap.petra)
                        .into(p_image );
            }

            p_sku.setText(product_array.get(position).sku);
            p_stock.setText(product_array.get(position).stock);
            p_stock.setSelection(p_stock.getText().length());
            product_name.setText(product_array.get(position).title);
            product_name.setSelection(product_name.getText().length());
            p_price.setText(product_array.get(position).price);
            p_price.setSelection(p_price.getText().length());
            p_description.setText(product_array.get(position).description);
            p_description.setSelection(p_description.getText().length());
            productid= product_array.get(position).id;

            try {
                JSONObject j_obj = new JSONObject();
                j_obj.put("ProductImageId", imageid);
                 j_arrayupdatearray=new JSONArray();
                j_arrayupdatearray.put(imageid);
            }catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        Button done = (Button) rowView.findViewById(R.id.done);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
/////////////////////////////////////////////////////
                Log.e("calling","validation");
                if(validation())
                {
                    Log.e("callied","validation");
                    if(flag){
                        setUrlPayload(Constants.UPDATEITEM,productid);

                    }
                    else
                    {
                        setUrlPayload(Constants.ADDITEM,"");
                    }
                    myuri=null;
                    alertDialog.dismiss();

                }

            }
        });
        Button back = (Button) rowView.findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        p_description.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if(validation())
                    {
                        Log.e("callied","validation");
                        if(flag){
                            setUrlPayload(Constants.UPDATEITEM,productid);

                        }
                        else
                        {
                            setUrlPayload(Constants.ADDITEM,"");
                        }
                        myuri=null;
                        alertDialog.dismiss();

                    }
                }
                return false;
            }
        });
                alertDialog.setContentView(rowView);
                alertDialog.setCancelable(true);
                alertDialog.setCanceledOnTouchOutside(true);
                alertDialog.show();

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            File croppedImageFile = new File(getFilesDir(), "test.jpg");
            if (requestCode == REQUEST_PICTURE && resultCode == RESULT_OK) {

                String copiedFilepath=null;
                try {
                    File actualImage = FileUtil.from(this, data.getData());
                    copiedFilepath=actualImage.getAbsolutePath();
                }catch (Exception e)
                {
                    util.customToast("Image Not Available");
                }
                File file = new File(copiedFilepath);
                long length = file.length();
                if(length>=307200) {
                    util.customToast("Image Size very big");
                    File compressedImage = new Compressor.Builder(this)
                            .setMaxWidth(640)
                            .setMaxHeight(480)
                            .setQuality(75)
                            .setCompressFormat(Bitmap.CompressFormat.PNG)
                            .setDestinationDirectoryPath(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_PICTURES).getAbsolutePath())
                            .build()
                            .compressToFile(file);
                    copiedFilepath=compressedImage.getAbsolutePath();
                }
                Intent intent = new Intent(this, ImageCropActivity.class);
                intent.putExtra(ImageCropActivity.EXTRA_X_RATIO, 26);///26,15 for banner
                intent.putExtra(ImageCropActivity.EXTRA_Y_RATIO, 26);
                intent.putExtra(ImageCropActivity.EXTRA_IMAGE_PATH, copiedFilepath);
                startActivityForResult(intent, REQUEST_CROP_PICTURE);
            } else if (requestCode == REQUEST_CROP_PICTURE && resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                if (bundle != null) {
                    String croppedImagePath = bundle.getString(ImageCropActivity.EXTRA_IMAGE_PATH);
                    if(flag)
                    {
                        imageupdate=true;
                    }
                    myuri = Uri.parse(croppedImagePath);
                    Picasso.get().load(new File(croppedImagePath)).memoryPolicy(MemoryPolicy.NO_CACHE).into(p_image);
                }
            } else if (requestCode == REQUEST_CAMERA) {
                if (resultCode != RESULT_CANCELED) {
                    if (imageToUploadUri != null) {
                       /* Uri croppedImage = imageToUploadUri;
                        Intent intent = new Intent(this, ImageCropActivity.class);
                        String picturePath = util.getRealPathFromURI(this, croppedImage);
                        String copiedFilepath = FileUtils.getInstance().copyFile(picturePath, FileUtils.getInstance().getFilePath(this, FileUtils.MEDIA_TYPE.PICTURE));
                        intent.putExtra(ImageCropActivity.EXTRA_X_RATIO, 26);///26,15 for banner
                        intent.putExtra(ImageCropActivity.EXTRA_Y_RATIO, 26);
                        intent.putExtra(ImageCropActivity.EXTRA_IMAGE_PATH, copiedFilepath);
                        startActivityForResult(intent, REQUEST_CROP_PICTURE);*/
                        String copiedFilepath=null;
                        Uri croppedImage = imageToUploadUri;
                        String picturePath = util.getRealPathFromURI(this, croppedImage);
                        copiedFilepath = FileUtils.getInstance().copyFile(picturePath, FileUtils.getInstance().getFilePath(this, FileUtils.MEDIA_TYPE.PICTURE));
                        File file = new File(copiedFilepath);
                        util.customToast("Image Size very big");
                        File compressedImage = new Compressor.Builder(this)
                                .setMaxWidth(1280)
                                .setMaxHeight(960)
                                .setQuality(75)
                                .setCompressFormat(Bitmap.CompressFormat.PNG)
                                .setDestinationDirectoryPath(Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_PICTURES).getAbsolutePath())
                                .build()
                                .compressToFile(file);
                        copiedFilepath=compressedImage.getAbsolutePath();
                        Intent intent = new Intent(this, ImageCropActivity.class);
                        intent.putExtra(ImageCropActivity.EXTRA_X_RATIO, 26);///26,15 for banner
                        intent.putExtra(ImageCropActivity.EXTRA_Y_RATIO, 26);
                        intent.putExtra(ImageCropActivity.EXTRA_IMAGE_PATH, copiedFilepath);
                        startActivityForResult(intent, REQUEST_CROP_PICTURE);
                    }
                }
            }
        }else
        {
            if(!imageupdate) {
                myuri = null;
                Picasso.get()
                        .load(myuri)
                        .placeholder(R.mipmap.petra)
                        .error(R.mipmap.petra)
                        .into(p_image);
            }
        }
    }

    /**Return image path of given URI*/

    private boolean validation(){
        boolean flag = true;
        /*if(myuri==null)
        {
            util.customToast("Please add your product's Image.");
            flag = false;
        }
        else*/ if(p_stock.getText().toString().trim().length()==0){
            // product_name.setError("Product Name should not be null!");
            util.customToast(getResources().getString(R.string.productcodenull));
            p_stock.requestFocus();
            flag = false;
        }
       else if(product_name.getText().toString().trim().length()==0){
           // product_name.setError("Product Name should not be null!");
            util.customToast(getResources().getString(R.string.productnamenull));
            product_name.requestFocus();
            flag = false;
        }
       else if (!(p_price.getText().toString().trim().length()>0)&& TextUtils.isDigitsOnly(p_price.getText().toString().trim())){
           // p_price.setError("Digit Only!");
            util.customToast(getResources().getString(R.string.productprice));
            p_price.requestFocus();

        }
        else if(p_description.getText().toString().trim().length()==0){
            util.customToast(getResources().getString(R.string.descriptionnull));
            p_description.requestFocus();
            flag = false;
        }
        return flag;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.add) {
            if (product_array.size()==10){
                util.customToast("Sorry!!You can add Only 10 Products");
            }
            else {
                addData(false, 0);
            }
            return true;
        }
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }


        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate( R.menu.menu_add, menu );
        MenuItem addOption = menu.findItem(R.id.add);
        addOption.setVisible(true);
        return true;
    }
    public void setUrlPayload(int category,String extra) {
        switch (category){
            case Constants.GETLIST_1:
                JSONObject j_obj = new JSONObject();
                try {
                    j_obj.put("merchant_location_id", merchant_location_id);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jsonwebrequest(Constants.CategoryAPI,j_obj,Constants.GETLIST_1,Constants.VIEW);
                break;
            case Constants.GETLIST_2:
                j_obj = new JSONObject();
                try {
                    j_obj.put("category_id", extra);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jsonwebrequest(Constants.CategoryAPI,j_obj,Constants.GETLIST_2,Constants.VIEW_SUB);
                break;
            case Constants.GETLIST:
                j_obj = new JSONObject();
                try {
                    j_obj.put("category_id", extra);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jsonwebrequest(Constants.ProductAPI,j_obj,Constants.GETLIST,Constants.VIEW);
                break;
            case  Constants.ADDITEM:
                Log.e("calliang","Add Product");
                //sendDataToServer(Constants.b_Add_Product);
                j_obj = new JSONObject();
                try {
                    JSONObject data = new JSONObject();
                    data.put("merchant_location_id", merchant_location_id);
                    data.put("admin_id", admin_id);
                    JSONObject categ = new JSONObject();
                    categ.put("title", product_name.getText().toString().trim());
                    categ.put("description", p_description.getText().toString().trim());
                    try {
                        categ.put("category", sub_categories.get(spn_subcategory.getSelectedItemPosition()).id);
                    }catch (Exception e ){
                        e.printStackTrace();
                        categ.put("category", categories.get(spn_category.getSelectedItemPosition()).id);
                    }
                    categ.put("stock", p_stock.getText().toString().trim());
                    categ.put("sku", p_sku.getText().toString().trim());
                    categ.put("price", p_price.getText().toString().trim());
                    categ.put("img", util.getBase64(p_image));
                    JSONArray categories =new JSONArray();
                    categories.put(categ);
                    data.put("products",categories);
                    j_obj.put("data",data);
                    jsonwebrequest(Constants.ProductAPI,j_obj,Constants.ADDITEM,Constants.CREATE);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                break;

            case  Constants.UPDATEITEM:
                Log.e("calliang","Add Product");
                //sendDataToServer(Constants.b_Add_Product);
                j_obj = new JSONObject();
                try {
                    JSONObject data = new JSONObject();
                    data.put("merchant_location_id", merchant_location_id);
                    data.put("admin_id", admin_id);
                    JSONObject categ = new JSONObject();
                    categ.put("id", productid);
                    categ.put("title", product_name.getText().toString().trim());
                    categ.put("description", p_description.getText().toString().trim());
                    try {
                        categ.put("category", sub_categories.get(spn_subcategory.getSelectedItemPosition()).id);
                    }catch (Exception e ){
                        e.printStackTrace();
                        categ.put("category", categories.get(spn_category.getSelectedItemPosition()).id);
                    }
                    categ.put("stock", p_stock.getText().toString().trim());
                    categ.put("sku", p_sku.getText().toString().trim());
                    categ.put("price", p_price.getText().toString().trim());
                    categ.put("img", util.getBase64(p_image));
                    JSONArray categories =new JSONArray();
                    categories.put(categ);
                    data.put("products",categories);
                    j_obj.put("data",data);
                    jsonwebrequest(Constants.ProductAPI,j_obj,Constants.UPDATEITEM,Constants.UPDATE);

                } catch (JSONException e) {
                    e.printStackTrace();
                }


                break;



            case  Constants.DELETEITEM:

                j_obj = new JSONObject();
                try {
                    JSONObject data = new JSONObject();
                    data.put("merchant_location_id", merchant_location_id);
                    data.put("admin_id", admin_id);
                    JSONObject categ = new JSONObject();
                    categ.put("id", productid);
                    JSONArray categories =new JSONArray();
                    categories.put(categ);
                    data.put("product_id",categories);
                    j_obj.put("data",data);
                    jsonwebrequest(Constants.ProductAPI,j_obj,Constants.DELETEITEM,Constants.DELETE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
        }



    }

    public void setReturnedData(int category, JSONObject j_obj) {
        switch (category){

            case Constants.GETLIST_1:
                Log.e("Apitransect",""+j_obj);
                cd.hide();
                try {
                    JSONObject categData =j_obj;
                    JSONArray re_j_obj = categData.getJSONArray("categories");
                    categories=(new Categories(re_j_obj)).categories;
                    cate_array = new ArrayList<>();
                    for (Categories.Category cate : categories){
                        cate_array.add(cate.title);
                    }
                    ArrayAdapter<String> uTyperArrayAdapter = new ArrayAdapter<String>(this, R.layout.item_spn_selected, cate_array);
                    uTyperArrayAdapter.setDropDownViewResource(R.layout.item_spn);
                    spn_category.setAdapter(uTyperArrayAdapter);
                    spn_category.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            setUrlPayload(Constants.GETLIST_2, categories.get(i).id);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case Constants.GETLIST_2:
                Log.e("Apitransect",""+j_obj);
                cd.hide();
                try {
                    JSONObject categData =j_obj;
                    JSONArray re_j_obj = categData.getJSONArray("sub_categories");
                    if(re_j_obj.length()>0) {
                        cv_subcategory.setVisibility(View.VISIBLE);
                        sub_categories = (new SubCategories(re_j_obj)).sub_categories;
                        sub_cate_array = new ArrayList<>();
                        for (SubCategories.SubCategory cate : sub_categories) {
                            sub_cate_array.add(cate.title);
                        }
                        ArrayAdapter<String> uTyperArrayAdapter = new ArrayAdapter<String>(this, R.layout.item_spn_selected, sub_cate_array);
                        uTyperArrayAdapter.setDropDownViewResource(R.layout.item_spn);
                        spn_subcategory.setAdapter(uTyperArrayAdapter);
                        spn_subcategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                                setUrlPayload(Constants.GETLIST, sub_categories.get(i).id);
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> adapterView) {

                            }
                        });
                    }
                    else{
                        cv_subcategory.setVisibility(View.GONE);
                        setUrlPayload(Constants.GETLIST, categories.get(spn_category.getSelectedItemPosition()).id);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case Constants.GETLIST:
                Log.e("Apitransect",""+j_obj);
                JSONArray re_j_obj;
                try {
                    returnedData =j_obj;
                    re_j_obj = returnedData.getJSONArray("products");
                    ArrayList<Products.Product> mContentItemss=new ArrayList<>();

                    if(product_array.size()==0){
                        product_array = (new Products(re_j_obj)).products;
                        listview.setAdapter(adapter = new ProductAdapter(ProductActivity.this,product_array));

                    }
                    else {
                        product_array.clear();
                        ArrayList<Products.Product> json = (new Products(re_j_obj)).products;
                        for(int i =0 ;i<json.size();i++){
                            product_array.add(json.get(i));
                        }
                        adapter.notifyDataSetChanged();
                    }
                    if(re_j_obj.length()!=0) {
                        txt_producttitle.setText("Products" + " (" + re_j_obj.length() + ")");
                        listview.setVisibility(View.VISIBLE);
                        tv_no_record.setVisibility(View.GONE);
                    }
                    else
                    {
                        txt_producttitle.setText("Products" + "(0)");
                        listview.setVisibility(View.GONE);
                        tv_no_record.setVisibility(View.VISIBLE);

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;

            case Constants.ADDITEM:
                Log.e("Apitransect",""+j_obj);
                setUrlPayload(Constants.GETLIST, sub_categories.get(spn_subcategory.getSelectedItemPosition()).id);
                //fillData(2);
                break;
            case Constants.UPDATEITEM:
                Log.e("Apitransect",""+j_obj);
                setUrlPayload(Constants.GETLIST, sub_categories.get(spn_subcategory.getSelectedItemPosition()).id);
                //fillData(3);
                break;
            case Constants.DELETEITEM:
                Log.e("Apitransect",""+j_obj);
                setUrlPayload(Constants.GETLIST, sub_categories.get(spn_subcategory.getSelectedItemPosition()).id);
                break;
        }
    }



    public void jsonwebrequest(String url ,final JSONObject j_obj, final int action, final String action_text) {
        if (util.checkInternetConnection()) {
            cd.show("");

            StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>() {
                        public JSONObject data;

                        @Override
                        public void onResponse(String response) {
                            cd.hide();
                            Log.e("loadTrans", ""+response);
                            try {
                                if((new JSONObject(response)).getString("status").equalsIgnoreCase("1")) {

                                    JSONObject returnJSON =new JSONObject(response);
                                    if(!(new JSONObject(response)).isNull("data"))
                                        returnJSON = (new JSONObject(response)).getJSONObject("data");

                                    if(!(new JSONObject(response)).isNull("message"))
                                        util.customToast((new JSONObject(response)).getString("message"));


                                    setReturnedData(action, returnJSON);

                                }
                                else{
                                    cd.hide();
                                    util.customToast("Failed");
                                }

                            }
                            catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            cd.hide();
                            Toast.makeText(ProductActivity.this, error.toString(), Toast.LENGTH_LONG).show();
                        }
                    }) {

                @Override
                protected Map<String, String> getParams() {

                    Map<String, String> params = new HashMap<String, String>();
                    params.put("data",j_obj.toString());
                    params.put("action",action_text);
                    Log.e("getParams", String.valueOf(params));
                    return params;
                }
                @Override
                public Map<String, String> getHeaders(){
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("Content-Type","application/x-www-form-urlencoded");
                    return params;
                }
            };

            stringRequest.setRetryPolicy(new RetryPolicy() {
                @Override
                public int getCurrentTimeout() {
                    return 100000;
                }

                @Override
                public int getCurrentRetryCount() {
                    return 100000;
                }

                @Override
                public void retry(VolleyError error) throws VolleyError {

                }
            });
            RequestQueue requestQueue = Volley.newRequestQueue(ProductActivity.this);
            requestQueue.add(stringRequest);
        }
        else
        {
            util.customToast(getResources().getString(R.string.nointernet));

        }
    }



    public void showimage(int p){
        final Dialog Dialog = new Dialog(ProductActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        LayoutInflater inflater = getLayoutInflater();
        View rowView = (View) inflater.inflate(R.layout.banner_full_image, null);
        Rect displayRectangle = new Rect();
        Window window = getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
        rowView.setMinimumWidth((int)(displayRectangle.width() * 1f));
        rowView.setMinimumHeight((int)(displayRectangle.height() * 0.5f));
        TouchImageView imgview = (TouchImageView) rowView.findViewById(R.id.image_detail);
        if(product_array.get(position).image_full_path.trim().length()!=0)
        {
            Picasso.get()
                    .load(product_array.get(p).image_full_path.replace(" ","%20"))
                    .placeholder(R.mipmap.petra)
                    .error(R.mipmap.petra)
                    .into(imgview);
        }
        else{
            Picasso.get()
                    .load(R.mipmap.petra)
                    .placeholder(R.mipmap.petra)
                    .error(R.mipmap.petra)
                    .into(imgview);
        }


        imgview.setMaxZoom(4f);
      /*  Button backfullimg = (Button) rowView.findViewById(R.id.back_fullimg);
        backfullimg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog.dismiss();
            }
        });*/
        Dialog.setContentView(rowView);
        Dialog.setCancelable(true);
        Dialog.setCanceledOnTouchOutside(true);
        Dialog.show();
    }
    private void selectImage() {
        final CharSequence[] items = {"Take Photo", "Choose from Library",
                "Cancel" };
        AlertDialog.Builder builder = new AlertDialog.Builder(ProductActivity.this);
        builder.setTitle("Add Product Image!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result=util.checkPermission(ProductActivity.this);
                if (items[item].equals("Take Photo")) {
                    // userChoosenTask="Take Photo";
                    if(result)
                       // cameraIntent();
                        captureCameraImage();
                } else if (items[item].equals("Choose from Library")) {
                    //  userChoosenTask="Choose from Library";
                    if(result)
                        galleryIntent();
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }



   public void galleryIntent() {
       //startActivityForResult(MediaStoreUtils.getPickImageIntent(b_OffersActivity.this), REQUEST_PICTURE);
       myuri=null;
       Intent intent = new Intent();
       intent.setType("image/*");
       intent.setAction(Intent.ACTION_GET_CONTENT);
       startActivityForResult(Intent.createChooser(intent,
               "Select Picture"), REQUEST_PICTURE);
   }
    private void captureCameraImage() {
        myuri=null;
        Intent chooserIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File f = new File(Environment.getExternalStorageDirectory(), "POST_IMAGE.jpg");
        chooserIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
        imageToUploadUri = Uri.fromFile(f);
        startActivityForResult(chooserIntent, REQUEST_CAMERA);
    }

    public void myClickMethod(final View v) {
        // /Function purpose to Perform ClickEvent of Element Based on view
        // /PARAM 1.v=view of the clicked Element....
        switch (v.getId()) {

/*
            case R.id.btnOkPopup:
                util.ButtonClickEffect(v);
                cd.hide();
                if(adapter!=null)
                    adapter.cd.hide();
                break;
*/
            case R.id.btn_ok:
                util.ButtonClickEffect(v);
                cd.hide();
                setUrlPayload(Constants.DELETEITEM,productid);
                if(adapter!=null)
                    adapter.cd.hide();
                cd.hide();
                break;              /*

            case R.id.btnwifi:
                util.ButtonClickEffect(v);
                boolean result=util.checkPermissionwifi(ProductActivity.this);
                if(result) {
                    WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                    if (!wifi.isWifiEnabled()) {
                        wifi.setWifiEnabled(true);
                        cd.hide();
                    }
                }
                if(adapter!=null)
                    adapter.cd.hide();
                break;
              */

             /* case R.id.btnreconnect:
                util.ButtonClickEffect(v);
                if(adapter!=null)
                    adapter.cd.hide();
                cd.hide();
                setUrlPayload(Constants.DELETEITEM, Integer.parseInt(product_array.get(position)[1]));
                break*/

            case R.id.btn_cancel:
                util.ButtonClickEffect(v);
                cd.hide();
                if(adapter!=null)
                    adapter.cd.hide();
                break;
        }
    }


}
