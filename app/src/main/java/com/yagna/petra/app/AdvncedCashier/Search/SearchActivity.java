package com.yagna.petra.app.AdvncedCashier.Search;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.cunoraz.tagview.Tag;
import com.cunoraz.tagview.TagView;
import com.yagna.petra.app.Admin.Transaction.TransactionFragment;
import com.yagna.petra.app.AdvncedCashier.DataKeeper;
import com.yagna.petra.app.AdvncedCashier.Product.ProductActivity;
import com.yagna.petra.app.AdvncedCashier.Product.ProductAdapter;
import com.yagna.petra.app.AdvncedCashier.SubCategory.SubCategoryActivity;
import com.yagna.petra.app.AdvncedCashier.SubCategory.SubCategoryReAdapter;
import com.yagna.petra.app.GlobalActivity;
import com.yagna.petra.app.Model.Cashiers;
import com.yagna.petra.app.Model.Product.Categories;
import com.yagna.petra.app.Model.Product.Discount;
import com.yagna.petra.app.Model.Product.Modifier;
import com.yagna.petra.app.Model.Product.Products;
import com.yagna.petra.app.Model.Product.SubCategories;
import com.yagna.petra.app.R;
import com.yagna.petra.app.Util.Constants;
import com.yagna.petra.app.Util.CustomDialog;
import com.yagna.petra.app.Util.PrefUtil;
import com.yagna.petra.app.Util.Utils;
import com.yagna.petra.app.views.TypefacedTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This activity controls the maint flow for the Custom Searchable behaviour. Its onCreate method
 * initialize the main UI components - such as the app bar and result list (RecyclerView). It should
 * be called through an intent and it's responses are also sent as intents
 */
public class SearchActivity extends GlobalActivity {
    // CONSTANTS
    private static final String TAG = "SearchActivity";
    public static final int VOICE_RECOGNITION_CODE = 1;
    public int[] selected_left={0,0,0,0};

    // UI ELEMENTS
    private RecyclerView searchResultList;
    public AutoCompleteTextView searchInput;
    private RelativeLayout voiceInput;
    private RelativeLayout dismissDialog;
    private ImageView micIcon;

    private String query;
    private String providerName;
    private String providerAuthority;
    private String searchableActivity;
    private Boolean isRecentSuggestionsProvider = Boolean.TRUE;
    private RelativeLayout rl_bn_search;
    private SearchProductAdapter adapter;
    public SearchAdapter cat_adapter;

    private SharedPreferences common_mypref;
    private Utils util;
    private CustomDialog cd;
    public ArrayList<Integer> select_array=new ArrayList<>();
    private String merchantid="",cashierid="",locationid="";
    private JSONObject returnedData;
    public static TagView searchTagGroup;
    private static LinearLayout ly_searchtag;
    private FloatingActionButton fab_cart;
    private RecyclerView.LayoutManager storiesLayoutManager;
    private boolean mTwoPane;
    public TypefacedTextView txt_all;

    private RecyclerView.LayoutManager subLayoutManager;
    public ArrayList<Categories.Category> cate_items;
    private  ProductFragment tr_fragment;


    // Activity Callbacks __________________________________________________________________________
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.custom_searchable);
        common_mypref = getApplicationContext().getSharedPreferences(
                "user", 0);

        if (findViewById(R.id.item_detail_container) != null) {
            mTwoPane = true;
        }
        else
            mTwoPane = false;

        String[] str= PrefUtil.getSearch(common_mypref).split("<->");
        DataKeeper.old_tagsArray = new ArrayList<>();
        for(int i=0; i<str.length;i++)
        {   if(str[i].trim().length()!=0)
            DataKeeper.old_tagsArray.add(str[i]);
        }
        this.query = "";
        this.searchResultList = (RecyclerView) this.findViewById(R.id.cs_result_list);
        searchResultList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.hideKeyboard(SearchActivity.this);
            }
        });
        this.searchInput = (AutoCompleteTextView) this.findViewById(R.id.custombar_text);

       /* if( getIntent().getExtras().getString("arg").length()>0){
            this.searchInput.setText( getIntent().getExtras().getString("arg"));
            this.searchInput.setSelection( getIntent().getExtras().getString("arg").length());
        }*/
        common_mypref = getApplicationContext().getSharedPreferences("user", 0);
        util=new Utils(this);
        cd = new CustomDialog(this);
        ArrayAdapter<String> suggestAdapter = new ArrayAdapter<String>
                (this,R.layout.simple_list_item_1, DataKeeper.old_tagsArray);
        searchInput.setAdapter(suggestAdapter);
        this.voiceInput = (RelativeLayout) this.findViewById(R.id.custombar_mic_wrapper);
        this.rl_bn_search = (RelativeLayout) this.findViewById(R.id.rl_bn_search);

        this.dismissDialog = (RelativeLayout) this.findViewById(R.id.custombar_return_wrapper);
        this.micIcon = (ImageView) this.findViewById(R.id.custombar_mic);
        this.micIcon.setSelected(Boolean.FALSE);
        initializeUiConfiguration();
        // Initialize result list
        storiesLayoutManager = createLayoutManager(getResources());
        searchResultList.setLayoutManager(storiesLayoutManager);
        //adapter = new SearchAdapter(this,DataKeeper.old_tagsArray);
        //searchResultList.setAdapter(adapter);
        setupSearchTaglayout();
        this.searchInput.setMaxLines(1);
        try {
            JSONObject jobj = new JSONObject(PrefUtil.getLoginData(common_mypref));
            Cashiers cashier = new Cashiers(jobj.getJSONObject("cashier"));
            merchantid=cashier.merchant_id;
            cashierid=cashier.cashier_id;
            locationid=cashier.merchant_location_id;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        fab_cart = (FloatingActionButton)findViewById(R.id.fab_cart);
        fab_cart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();

            }
        });

        if (mTwoPane)
        {
            tr_fragment = new ProductFragment();
            Bundle arguments = new Bundle();
            arguments.putString(TransactionFragment.ARG_ITEM_ID, "");
            tr_fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.item_detail_container, tr_fragment)
                    .commit();
        }

        implementSearchTextListener();
        implementDismissListener();
        implementVoiceInputListener();

        txt_all = findViewById(R.id.txt_all);
        txt_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                DataKeeper.search_product_array.clear();
                for(int i =0 ;i<DataKeeper.all_products_array.size();i++){
                    DataKeeper.search_product_array.add(DataKeeper.all_products_array.get(i));
                }
                txt_all.setTextColor(getResources().getColor(R.color.ink_blue));
                for (int j = 0; j < cate_items.size(); j++)
                    for (int i = 0; i < cate_items.get(j).subCategories.size(); i++) {
                        cate_items.get(j).subCategories.get(i).isSelected = false;
                    }
                    cat_adapter.notifyDataSetChanged();
                fillProducts("",true);


            }
        });
        txt_all.setTextColor(getResources().getColor(R.color.ink_blue));
        getAllProducts();

        // implementResultListOnItemClickListener();

       // getManifestConfig();
    }

    private RecyclerView.LayoutManager createLayoutManager(Resources resources) {
        int spans = 1;/*resources.getInteger(R.integer.feed_columns);*/
        return new StaggeredGridLayoutManager(spans, RecyclerView.VERTICAL);
    }
    private void getAllProducts(){

        setUrlPayload(Constants.GETLIST_3,0);
    }
    private void fillCategories(int i){
        Log.e("called by",""+i);
        setUrlPayload(Constants.GETLIST,0);
    }
    public void fillSubCategories(int i){
        Log.e("called by",""+i);
        setUrlPayload(Constants.GETLIST_2,i);
    }
    public void ProdsAndSubCategories(int i){
        Log.e("called by",""+i);
        setUrlPayload(Constants.GETLIST_1,i);
    }
    // Receives the intent with the speech-to-text information and sets it to the InputText
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case VOICE_RECOGNITION_CODE: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    searchInput.setText(text.get(0));
                }
                break;
            }
        }
    }

    // Sends an intent with the typed query to the searchable Activity
    private void sendSuggestionIntent(ResultItem item) {
        try {
            Intent sendIntent = new Intent(this, Class.forName(searchableActivity));
            sendIntent.setAction(Intent.ACTION_VIEW);
            sendIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            Bundle b = new Bundle();
            b.putParcelable(CustomSearchableConstants.CLICKED_RESULT_ITEM, item);

            sendIntent.putExtras(b);
            startActivity(sendIntent);

            finish();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Sends an intent with the typed query to the searchable Activity
    private void sendSearchIntent(String trim) {
        boolean wannAdd=true;
        for(int i = 0; i<DataKeeper.old_tagsArray.size(); i++){
            if(DataKeeper.old_tagsArray.get(i).equalsIgnoreCase(trim)){
                wannAdd=false;
                break;
            }
        }
        if(wannAdd)
            DataKeeper.old_tagsArray.add(trim);
        saveSearchData();

        //AdvCashierActivity.isSearch=true;
        if(searchInput.getText().toString().trim().length()!=0)
            if(mTwoPane)
            {
                tr_fragment.callFreeSearch(searchInput.getText().toString(),locationid);

            }
            else{
                Intent intent=new Intent(SearchActivity.this, ProductSingleViewActivity.class);
                intent.putExtra(ProductFragment.ARG_SEARCH, ""+searchInput.getText().toString());
                intent.putExtra(ProductFragment.LOC_ID, locationid);


                startActivity(intent);
            }



        //finish();
      /* try {
            Intent sendIntent = new Intent(this, Class.forName(searchableActivity));
            sendIntent.setAction(Intent.ACTION_SEARCH);
            sendIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            sendIntent.putExtra(SearchManager.QUERY, query);

            // If it is set one-line mode, directly saves the suggestion in the provider
            if (!CustomSearchableInfo.getIsTwoLineExhibition()) {
                SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, providerAuthority, SearchRecentSuggestionsProvider.DATABASE_MODE_QUERIES);
                suggestions.saveRecentQuery(query, null);
            }

            startActivity(sendIntent);
            finish();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } */
    }



    public void saveSearchData() {
        String saveme="";
        for(int i = 0; i<DataKeeper.old_tagsArray.size(); i++)
            saveme=i==0?DataKeeper.old_tagsArray.get(i):saveme+"<->"+DataKeeper.old_tagsArray.get(i);
        PrefUtil.saveSearch(common_mypref,saveme);
    }

    // Listeners implementation ____________________________________________________________________
    private void implementSearchTextListener() {
        // Gets the event of pressing search button on soft keyboard
        TextView.OnEditorActionListener searchListener = new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    sendSearchIntent(exampleView.getText().toString().trim());
                }
                return true;
            }
        };
        rl_bn_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendSearchIntent(searchInput.getText().toString().trim());

            }
        });

        searchInput.setOnEditorActionListener(searchListener);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(final CharSequence s, int start, int before, int count) {
                if (!"".equals(searchInput.getText().toString())) {
                    query = searchInput.getText().toString();

                    setClearTextIcon();
                    /*if (isRecentSuggestionsProvider) {
                        // Provider is descendant of SearchRecentSuggestionsProvider
                       // mapResultsFromRecentProviderToList();
                    } else {
                        // Provider is custom and shall follow the contract
                        mapResultsFromCustomProviderToList();
                    }*/
                } else {
                    setMicIcon();
                    setReturnedData(Constants.GETLIST,returnedData, 0);
                }
            }

            // DO NOTHING
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            // DO NOTHING
            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub

            }
        });
    }

    // Finishes this activity and goes back to the caller
    private void implementDismissListener () {
        this.dismissDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    // Implements speech-to-text
    private void implementVoiceInputListener () {
        this.voiceInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (micIcon.isSelected()) {
                    searchInput.setText("");
                    query = "";
                    setReturnedData(Constants.GETLIST,returnedData, 0);
                    micIcon.setSelected(Boolean.FALSE);
                    micIcon.setImageResource(R.drawable.mic_icon);
                } else {
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now");

                    SearchActivity.this.startActivityForResult(intent, VOICE_RECOGNITION_CODE);
                }
            }
        });
    }

    // Sends intent to searchableActivity with the selected result item
    private void implementResultListOnItemClickListener () {
        searchResultList.addOnItemTouchListener(new RecyclerViewOnItemClickListener(this,
                new RecyclerViewOnItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                       String clickedItem = DataKeeper.old_tagsArray.get(position);
                        sendSearchIntent(clickedItem);
                    }
                }));
    }


    // UI __________________________________________________________________________________________
    // Identifies if client have set any of the configuration attributes, if yes, reset UI element source, toherwise keep defaul value
    private void initializeUiConfiguration () {
        // Set activity background transparency
        if (CustomSearchableInfo.getTransparencyColor() != CustomSearchableConstants.UNSET_RESOURCES) {
            LinearLayout activityWrapper = (LinearLayout) this.findViewById(R.id.custom_searchable_wrapper);
            activityWrapper.setBackgroundColor(CustomSearchableInfo.getTransparencyColor());
        }

        if (CustomSearchableInfo.getPrimaryColor() != CustomSearchableConstants.UNSET_RESOURCES) {
            RelativeLayout headerWrapper = (RelativeLayout) this.findViewById(R.id.cs_header);
            headerWrapper.setBackgroundColor(CustomSearchableInfo.getPrimaryColor());
        }

        if (CustomSearchableInfo.getSearchTextSize() != CustomSearchableConstants.UNSET_RESOURCES) {
            searchInput.setTextSize(TypedValue.COMPLEX_UNIT_PX, CustomSearchableInfo.getSearchTextSize());
        }

        if (CustomSearchableInfo.getTextPrimaryColor() != CustomSearchableConstants.UNSET_RESOURCES) {
            searchInput.setTextColor(CustomSearchableInfo.getTextPrimaryColor());
        }

        if (CustomSearchableInfo.getTextHintColor() != CustomSearchableConstants.UNSET_RESOURCES) {
            searchInput.setHintTextColor(CustomSearchableInfo.getTextHintColor());
        }

        if (CustomSearchableInfo.getBarDismissIcon() != CustomSearchableConstants.UNSET_RESOURCES) {
            ImageView dismissIcon = (ImageView) this.findViewById(R.id.custombar_return);
            dismissIcon.setImageResource(CustomSearchableInfo.getBarDismissIcon());
        }

        if (CustomSearchableInfo.getBarMicIcon() != CustomSearchableConstants.UNSET_RESOURCES) {
            ImageView micIcon = (ImageView) this.findViewById(R.id.custombar_mic);
            micIcon.setImageResource(CustomSearchableInfo.getBarMicIcon());
        }

        if (CustomSearchableInfo.getBarHeight() != CustomSearchableConstants.UNSET_RESOURCES) {
            RelativeLayout custombar = (RelativeLayout) this.findViewById(R.id.cs_header);
            android.view.ViewGroup.LayoutParams params = custombar.getLayoutParams();
            params.height = CustomSearchableInfo.getBarHeight().intValue();
            custombar.setLayoutParams(params);
        }
    }

    // Set X as the icon for the right icon in the app bar
    private void setClearTextIcon () {
        micIcon.setSelected(Boolean.TRUE);
        micIcon.setImageResource(R.drawable.delete_icon);
        micIcon.invalidate();
    }

    // Set the micrphone icon as the right icon in the app bar
    private void setMicIcon () {
        micIcon.setSelected(Boolean.FALSE);
        micIcon.setImageResource(R.drawable.mic_icon);
        micIcon.invalidate();
    }


    public void setUrlPayload(int action,int extra) {
        switch (action){

            case Constants.GETLIST:
                JSONObject j_obj = new JSONObject();
                try {
                    j_obj.put("merchant_location_id", locationid);


                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jsonwebrequest(j_obj,Constants.GETLIST,Constants.VIEW,0);
                break;

            case Constants.GETLIST_1:
                j_obj = new JSONObject();
                try {
                    j_obj.put("category_id", cate_items.get(extra).id );

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jsonwebrequest(j_obj,Constants.GETLIST_1,Constants.VIEW_SUB,extra);
                break;
            case Constants.GETLIST_2:
                j_obj = new JSONObject();
                try {
                    j_obj.put("category_id", cate_items.get(extra).id );

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jsonwebrequest(j_obj,Constants.GETLIST_2,Constants.VIEW_SUB,extra);
                break;

            case Constants.GETLIST_3:
                 j_obj = new JSONObject();
                try {
                    j_obj.put("merchant_location_id", locationid);


                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jsonwebrequest(j_obj,Constants.GETLIST_3,Constants.VIEW_All,0);
                break;

        }

    }

    public void setReturnedData(int category, JSONObject j_obj, int posit) {
        switch (category){
            case Constants.GETLIST:
                Log.e("Apitransect",""+j_obj);
                cd.hide();
                JSONArray re_j_obj;
                try {
                    returnedData =j_obj;
                    re_j_obj = returnedData.getJSONArray("categories");
                    cate_items=(new Categories(re_j_obj)).categories;
                    select_array = new ArrayList<>();
                    cat_adapter = new SearchAdapter(SearchActivity.this, cate_items,mTwoPane);
                    searchResultList.setAdapter(cat_adapter);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;

            case Constants.GETLIST_1:
                Log.e("Apitransect",""+j_obj);
                cd.hide();
                try {
                    returnedData =j_obj;
                    re_j_obj = returnedData.getJSONArray("sub_categories");
                    ArrayList<SubCategories.SubCategory> sub_categories=(new SubCategories(re_j_obj)).sub_categories;
                    cate_items.get(posit).subCategories = sub_categories;
                    cat_adapter.notifyDataSetChanged();
                    re_j_obj = returnedData.getJSONArray("products");
                    DataKeeper.search_product_array.clear();
                    ArrayList<Products.Product> json = (new Products(re_j_obj)).products;
                    for(int i =0 ;i<json.size();i++){
                        DataKeeper.search_product_array.add(json.get(i));
                    }
                    fillProducts("",true);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case Constants.GETLIST_2:
                Log.e("Apitransect",""+j_obj);
                cd.hide();
                try {
                    returnedData =j_obj;
                    re_j_obj = returnedData.getJSONArray("sub_categories");
                    ArrayList<SubCategories.SubCategory> sub_categories=(new SubCategories(re_j_obj)).sub_categories;
                    cate_items.get(posit).subCategories = sub_categories;
                    cat_adapter.notifyDataSetChanged();



                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case Constants.GETLIST_3:

                Log.e("Apitransect",""+j_obj);
                cd.hide();
                try {
                    returnedData =j_obj;
                    re_j_obj = returnedData.getJSONArray("products");
                    DataKeeper.search_product_array.clear();
                    ArrayList<Products.Product> data = (new Products(re_j_obj)).products;
                    DataKeeper.all_products_array=data;
                    for(int i =0 ;i<data.size();i++){
                        DataKeeper.search_product_array.add(data.get(i));
                    }
                    fillProducts("",true);
                    fillCategories(1);


                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;





        }
    }


    public void jsonwebrequest(final JSONObject j_obj, final int action, final String action_text, final int posit) {
        if (util.checkInternetConnection()) {
            cd.show("");
            String url =  Constants.CategoryAPI;
            if(action==Constants.GETLIST_3)
                url =  Constants.ProductAPI;

            Log.e("url ", ""+url);
            StringRequest stringRequest = new StringRequest(Request.Method.POST,url,
                    new Response.Listener<String>() {
                        public JSONObject data;

                        @Override
                        public void onResponse(String response) {
                            cd.hide();
                            Log.e("loadTrans", ""+response);
                            try {
                                if((new JSONObject(response)).getString("status").equalsIgnoreCase("1")) {

                                    JSONObject returnJSON =new JSONObject();
                                    if(!(new JSONObject(response)).isNull("data"))
                                        returnJSON = (new JSONObject(response)).getJSONObject("data");
                                    else
                                        returnJSON = (new JSONObject(response));

                                    if(!(new JSONObject(response)).isNull("message"))
                                        util.customToast((new JSONObject(response)).getString("message"));

                                    setReturnedData(action, returnJSON,posit);

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
                            Toast.makeText(SearchActivity.this, error.toString(), Toast.LENGTH_LONG).show();
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
            RequestQueue requestQueue = Volley.newRequestQueue(SearchActivity.this);
            requestQueue.add(stringRequest);
        }
        else
        {
            util.customToast(getResources().getString(R.string.nointernet));

        }
    }

    private void setupSearchTaglayout() {
        searchTagGroup =(TagView)findViewById(R.id.tag_group);
        ly_searchtag=(LinearLayout) findViewById(R.id.ly_searchtag);
        searchTagGroup.setOnTagClickListener(new TagView.OnTagClickListener() {
            @Override
            public void onTagClick(com.cunoraz.tagview.Tag tag, int i) {

            }
        });
        //set delete listener
        searchTagGroup.setOnTagDeleteListener(new TagView.OnTagDeleteListener() {
            @Override
            public void onTagDeleted(TagView tagView, com.cunoraz.tagview.Tag tag, int i) {
                searchTagGroup.remove(i);
                DataKeeper.product_name_selected.remove(i);
                DataKeeper.product_id_selected.remove(i);
                DataKeeper.products_array.remove(i);

            }
        });
        //set long click listener
        searchTagGroup.setOnTagLongClickListener(new TagView.OnTagLongClickListener() {
            @Override
            public void onTagLongClick(com.cunoraz.tagview.Tag tag, int i) {
            }
        });

        for(int i = 0;i<DataKeeper.product_name_selected.size();i++){
            com.cunoraz.tagview.Tag s_tag = new com.cunoraz.tagview.Tag(DataKeeper.product_name_selected.get(i));
            s_tag.isDeletable=true;
            s_tag.layoutColor = R.color.colorPrimaryDark;
            searchTagGroup.addTag(s_tag);
        }
        ly_searchtag.setVisibility(View.VISIBLE);

    }

    public static void addTag(Products.Product product){
        DataKeeper.product_id_selected.add(0,product.id);
        DataKeeper.product_name_selected.add(0,product.title);
        DataKeeper.products_array.add(0,product);
        List <Tag> tagiis = new ArrayList<>();
        searchTagGroup.removeAll();
        for(int i=0;i<DataKeeper.product_name_selected.size();i++ ){
            com.cunoraz.tagview.Tag s_tag = new com.cunoraz.tagview.Tag(DataKeeper.product_name_selected.get(i));
            s_tag.isDeletable=true;
            s_tag.layoutColor = R.color.colorPrimaryDark;
            tagiis.add(s_tag);
        }
        searchTagGroup.addTags(tagiis);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    public void dialogAsk(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme));
        builder.setMessage("Are you sure want to remove this search filter?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                searchTagGroup.removeAll();
                //String scieTag = storiesPagerAdapter.getTag( headersPager.getCurrentItem());
                //     String tag = storiesPagerAdapter.getTag(0);
                //   ScienceFragment fragment = (ScienceFragment) getSupportFragmentManager().findFragmentByTag(tag);
                // ScienceFragment fragment = (ScienceFragment) storiesPagerAdapter.getItem(0);

            }
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }

    public void getModifiers(final Products.Product item) {

        if (util.checkInternetConnection()) {
            cd.show("");

            StringRequest stringRequest = new StringRequest(Request.Method.POST, Constants.ModifierAPI,
                    new Response.Listener<String>() {
                        public JSONObject data;

                        @Override
                        public void onResponse(String response) {
                            cd.hide();
                            Log.e("loadTrans", ""+response);
                            try {
                                if((new JSONObject(response)).getString("status").equalsIgnoreCase("1")) {

                                    JSONObject returnJSON =new JSONObject(response);

                                    if(!(new JSONObject(response)).isNull("message"))
                                        util.customToast((new JSONObject(response)).getString("message"));

                                    JSONArray re_j_obj;
                                    try {
                                        returnedData =returnJSON;
                                        re_j_obj = returnedData.getJSONArray("data");

                                        ArrayList<Modifier> modif_array = new ArrayList<>();
                                        modif_array.clear();
                                        for(int i =0 ;i<re_j_obj.length();i++){
                                            try {
                                                modif_array.add(new Modifier(re_j_obj.getJSONObject(i)));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        item.modifier=modif_array;
                                        getDiscount(item);


                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }


                                }
                                else{
                                    cd.hide();
                                    util.customToast("Failed to fetch modifiers");
                                    getDiscount(item);
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
                            Toast.makeText(SearchActivity.this, error.toString(), Toast.LENGTH_LONG).show();
                        }
                    }) {

                @Override
                protected Map<String, String> getParams() {
                    final JSONObject j_obj = new JSONObject();
                    try {
                        j_obj.put("product_id", item.id);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Map<String, String> params = new HashMap<String, String>();
                    params.put("data",j_obj.toString());
                    params.put("action",Constants.VIEW);
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
            RequestQueue requestQueue = Volley.newRequestQueue(SearchActivity.this);
            requestQueue.add(stringRequest);
        }
        else
        {
            util.customToast(getResources().getString(R.string.nointernet));

        }
    }

    public void getDiscount(final Products.Product item) {
        if (util.checkInternetConnection()) {
            cd.show("");

            StringRequest stringRequest = new StringRequest(Request.Method.POST, Constants.DsicountAPI,
                    new Response.Listener<String>() {
                        public JSONObject data;

                        @Override
                        public void onResponse(String response) {
                            cd.hide();
                            Log.e("loadTrans", ""+response);
                            try {
                                if((new JSONObject(response)).getString("status").equalsIgnoreCase("1")) {

                                    JSONObject returnJSON =new JSONObject(response);

                                    if(!(new JSONObject(response)).isNull("message"))
                                        util.customToast((new JSONObject(response)).getString("message"));
                                    returnedData =returnJSON;
                                    JSONArray re_j_obj = returnedData.getJSONArray("data");
                                    ArrayList<Discount> disc_array =new ArrayList<>();
                                    for(int i =0 ;i<re_j_obj.length();i++){
                                        try {
                                            disc_array.add(new Discount(re_j_obj.getJSONObject(i)));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                      item.discount=disc_array;
                                    addTag(item);
                                    //util.customToast("Product Added");
                                }
                                else{
                                    cd.hide();
                                    util.customToast("Failed to fetch Discounts");
                                    addTag(item);
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
                            Toast.makeText(SearchActivity.this, error.toString(), Toast.LENGTH_LONG).show();
                        }
                    }) {

                @Override
                protected Map<String, String> getParams() {
                    JSONObject j_obj = new JSONObject();
                    try {
                        j_obj.put("product_id",item.id );

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("data",j_obj.toString());
                    params.put("action",Constants.VIEW);
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
            RequestQueue requestQueue = Volley.newRequestQueue(SearchActivity.this);
            requestQueue.add(stringRequest);
        }
        else
        {
            util.customToast(getResources().getString(R.string.nointernet));

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (item.getItemId()) {
            case android.R.id.home:

                finish();
                break;
        }


        return super.onOptionsItemSelected(item);
    }

    public void fillProducts(String id,boolean isSaved) {
            if(mTwoPane)
            {
              tr_fragment.fillData(id, isSaved);
            }
            else{
                    Intent intent=new Intent(SearchActivity.this, ProductSingleViewActivity.class);
                    intent.putExtra(ProductFragment.ARG_ITEM_ID, ""+id);
                     intent.putExtra(ProductFragment.ARG_TYPE, ""+isSaved);

                startActivity(intent);
           }
    }
}

