package com.sap.appodatav4.mdui.buttonselect;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sap.appodatav4.R;
import com.sap.appodatav4.mdui.EntitySetListActivity;
import com.sap.appodatav4.mdui.FunctionList;


public class ButtonSelect extends AppCompatActivity implements ButtonSelectItemAdapter.ItemClickListener {

    ButtonSelectItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_button_select);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Context context = ButtonSelect.this;

        String[] data = getIntent().getStringArrayExtra("data");


        // RecyclerView
        RecyclerView recyclerView = findViewById(R.id.rvNumbers);
        int numberOfColumns = 2;
        recyclerView.setLayoutManager(new GridLayoutManager(this, numberOfColumns));
        adapter = new ButtonSelectItemAdapter(this, data);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(View view, int position) {
        Log.i("itemcliccato", "hai cliccato il numero" + adapter.getItem(position) + ", con posizione " + position);
        Toast.makeText(this, "hai cliccato la funzione " + adapter.getItem(position) + ", con posizione " + position, Toast.LENGTH_LONG).show();

        String button = adapter.getItem(position);

        switch (button){
            case "Ordini":
                Intent intent = new Intent(this, FunctionList.class);
                startActivity(intent);
                break;
            case "Immessi":
                Intent intent2 = new Intent(this, EntitySetListActivity.class);
                startActivity(intent2);
                break;
            default:
                break;
        }
        /*
        Intent intent = new Intent(this, EntitySetListActivity.class);

        startActivity(intent);
    */
    }


}
