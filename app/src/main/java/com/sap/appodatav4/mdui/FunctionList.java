package com.sap.appodatav4.mdui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.sap.appodatav4.R;

import java.util.ArrayList;
import java.util.List;

public class FunctionList extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_function_list);
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Funzione");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());

        ListView listView = findViewById(R.id.function_listview);

        ArrayList<String> arrayList = new ArrayList<String>();

        arrayList.add("Conferma Bolle");
        arrayList.add("Resi Unificati");
        arrayList.add("Ricevimenti OODD");


        FunctionListAdapter adapter2 = new FunctionListAdapter(this, arrayList);

        listView.setAdapter(adapter2);
    }
    public class FunctionListAdapter extends ArrayAdapter<String> {

        public FunctionListAdapter(@NonNull Context context, ArrayList<String> arrayList) {
            super(context, 0, arrayList);

        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            String item = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.element_function_list, parent, false);
            }

            TextView textView = (TextView) convertView.findViewById(R.id.single_element);

            textView.setText(item);

            return convertView;
        }
    }

}
