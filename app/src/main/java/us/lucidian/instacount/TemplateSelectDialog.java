package us.lucidian.instacount;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class TemplateSelectDialog extends Activity {

    public static final String    POSITION            = "POSITION";
    public static final String[]  CROP_TEMPLATE_TEXT  = new String[]{"Square", "Rectangle", "Circle"};
    public static final Integer[] CROP_TEMPLATE_IMAGE = new Integer[]{R.drawable.crop_square, R.drawable.crop_rectangle, R.drawable.crop_circle};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.template_select_dialog);

        // Set result CANCELED in-case the user backs out
        setResult(Activity.RESULT_CANCELED);

        ListView mListView = (ListView) findViewById(R.id.template_select_list);
        mListView.setAdapter(new CropTemplateListViewAdapter(this, CROP_TEMPLATE_TEXT, CROP_TEMPLATE_IMAGE));
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Intent intent = new Intent();
                intent.putExtra(POSITION, position);

                // Set result and finish this Activity.
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
    }
    
    // Adapter that will setup the layout of each line in ListView.
    private class CropTemplateListViewAdapter extends BaseAdapter {
        private Context mmContext;
        private String[] mmTypes;
        private Integer[] mmImages;
        private LayoutInflater layoutInflator;

        public CropTemplateListViewAdapter(Context context, String[] types, Integer[] imgs) {
            mmContext = context;
            mmTypes = types;
            mmImages = imgs;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            layoutInflator = (LayoutInflater) mmContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = layoutInflator.inflate(R.layout.crop_template_listview_layout, parent, false);

            TextView textview = (TextView) row.findViewById(R.id.ft_crop_template_label);
            ImageView imageview = (ImageView) row.findViewById(R.id.ft_crop_template_image);

            textview.setText(mmTypes[position]);
            imageview.setImageResource(mmImages[position]);

            return (row);
        }

        @Override
        public int getCount() {
            return mmTypes.length;
        }

        @Override
        public Object getItem(int arg0) {
            return null;
        }

        @Override
        public long getItemId(int arg0) {
            return 0;
        }
    }
}