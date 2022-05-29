package com.example.team17zooseeker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import org.jgrapht.Graph;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.view.View;
import android.widget.Button;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private final String STATE_ITINERARY = "1";
    private final String STATE_DIRECTIONS = "2";

    private ZooKeeperDatabase database;
    private NodeItemDao nodeDao;
    private EdgeItemDao edgeDao;
    private StateDao stateDao;

    private Map<String, nodeItem> nodeMap;

    private ArrayList<String> visitationList;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private List<nodeItem> addedNodesList = new ArrayList<nodeItem>();
    private NodeListAdapter adapter = new NodeListAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        database = ZooKeeperDatabase.getSingleton(this);

        nodeDao = database.nodeItemDao();
        edgeDao  = database.edgeItemDao();
        stateDao = database.stateDao();

        //TESTING
//        stateDao.delete(stateDao.get());
//        stateDao.insert(new State("0"));

        State state = stateDao.get();

        if(state == null) {
            stateDao.insert(new State("0"));
            state = stateDao.get();
        }

        //List<edgeItem> edges = edgeDao.getAll();
        //List<nodeItem> nodes = nodeDao.getAll();
        List<edgeItem> edges;
        List<nodeItem> nodes;

        Map<String, nodeItem> nodeZ = null;

        try {
            nodeZ = nodeItem.loadNodeInfoJSON(this, "node.json");
            //state = State.loadStateInfoJSON(context, "state.json");

        } catch (IOException e) {
            e.printStackTrace();
        }

        nodes = new ArrayList<nodeItem>(nodeZ.values());

        //DELETEBADBADBAD
        //nodeDao.insertAll(nodes);

        // For MainActivity
        preferences = getPreferences(MODE_PRIVATE);
        editor = preferences.edit();

        Set<String> vSet = preferences.getStringSet("visitationList", null);

        //Database stuff

        this.nodeMap = nodes.stream().collect(Collectors.toMap(nodeItem::getName, Function.identity()));

        //Visitation List recycler
        adapter.setHasStableIds(true);

        RecyclerView visitationView = findViewById(R.id.visitation_list_view);
        visitationView.setLayoutManager(new LinearLayoutManager(this));
        visitationView.setAdapter(adapter);

        //Adding an exhibit to visitation list
        EditText searchText = findViewById(R.id.search_text);
        TextView exhibitText = findViewById(R.id.exhibit_count_txt);

        if(vSet != null) {

            visitationList = new ArrayList(vSet);

            for(String node : vSet)
                addedNodesList.add(nodeMap.get(node));

            adapter.setNodeItems(addedNodesList);

            exhibitText.setText("( " + visitationList.size() + " )");

        }

        else {
            this.visitationList = new ArrayList<String>();
        }

        //Itinerary State Check
        if(state.state.equals(STATE_ITINERARY)) {

            for(int i = 0; i < this.visitationList.size(); i++){
                this.visitationList.set(i, this.nodeMap.get(this.visitationList.get(i)).getId());
            }

            this.visitationList.clear();
            editor.putStringSet("visitationList", null);
            editor.apply();

            Intent intent = new Intent(this, ItineraryActivity.class);

            startActivity(intent);
            finish();

        }

        else if(state.state.equals(STATE_DIRECTIONS)) {

            Intent intent = new Intent(this, DirectionsActivity.class);

            startActivity(intent);
            finish();

        }

        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                int result = actionId & EditorInfo.IME_MASK_ACTION;

                if(result == EditorInfo.IME_ACTION_DONE) {
                    String searchQuery = searchText.getText().toString();

                    if (nodeMap.containsKey(searchQuery) &&
                            !(visitationList.contains(searchQuery))) {

                        addedNodesList.add(nodeMap.get(searchQuery));
                        visitationList.add(nodeMap.get(searchQuery).getName());

                        editor.putStringSet("visitationList", new HashSet(visitationList));
                        editor.apply();

                        adapter.setNodeItems(addedNodesList);

                        exhibitText.setText("( " + visitationList.size() + " )");
                    }
                }
                searchText.setText("");
                return true;
            }
        });

        Button plan = findViewById(R.id.plan_btn);
        plan.setOnClickListener(this::onPlanClicked);

        Button clear = findViewById(R.id.clear_btn);
        clear.setOnClickListener(this::onClearClicked);

        //Setting up the autocomplete text field with custom adapter
        AutoCompleteTextView searchTextView = (AutoCompleteTextView)findViewById(R.id.search_text);
        ArrayAdapter<String> autoCompleteAdapter = new AutoCompleteAdapter(this);
        searchTextView.setAdapter(autoCompleteAdapter);
    }

    void onClearClicked(View view) {
        TextView exhibitText = findViewById(R.id.exhibit_count_txt);

        visitationList.clear();
        addedNodesList.clear();

        editor.putStringSet("visitationList", new HashSet(visitationList));
        editor.apply();

        adapter.setNodeItems(addedNodesList);
        exhibitText.setText("( " + visitationList.size() + " )");
    }

    void onPlanClicked (View view){
        Log.d("Visitation List: ", this.visitationList.toString());
        if(visitationList.size()==0){
            Utilities.showAlert(this, "Please add Exhibits to your Visitation Plan :D");
            return;
        }
        //Visitation List needs to be in Ids and not names
        for(int i = 0; i < this.visitationList.size(); i++){
            this.visitationList.set(i, this.nodeMap.get(this.visitationList.get(i)).getId());
        }

        Intent intent = new Intent(this, ItineraryActivity.class);
        intent.putStringArrayListExtra("VList", this.visitationList);

        Itinerary.createItinerary(this, new ArrayList<>(visitationList));

        editor.putStringSet("visitationList", null);
        editor.apply();

        //this.visitationList.clear();


        startActivity(intent);
        finish();
    }
}