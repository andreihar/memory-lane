package com.itjustworks.memorylane;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Objects;

/*
 * Analysis.java
 *
 * Class Description: Creates a pie chart of the weights of each question set letting users compare
 * how accurate they are answering each set. Then weights are saved in the Firebase dataset and are
 * calculated in RandomAlgorithm.
 * Class Invariant: Weights of question set is in Firebase
 *                  Question sets are complete
 *
 */

public class Analysis extends AppCompatActivity {

    private PieChart pieChart;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        TextView noConnectivity = findViewById(R.id.no_wifi_analysis);
        ImageView noConnectivityIcon = findViewById(R.id.no_wifi_analysis_icon);
        pieChart = findViewById(R.id.piechart);
        boolean connected;
        // we are connected to a network
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        connected = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED;
        if (connected) {
            noConnectivity.setVisibility(View.GONE);
            noConnectivityIcon.setVisibility(View.GONE);
            pieChart.setVisibility(View.VISIBLE);
            databaseReference = FirebaseDatabase.getInstance().getReference("questionSets");
            setupPieChart();
            loadPieChartData();
        }
    }

    private void setupPieChart() {
        pieChart.setDrawHoleEnabled(true);
        pieChart.setUsePercentValues(true);
        pieChart.setEntryLabelTextSize(12);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setCenterText(getString(R.string.pie_chart_title));
        pieChart.setCenterTextSize(24);
        pieChart.getDescription().setEnabled(false);

        Legend l = pieChart.getLegend();
        l.setEnabled(false);
    }

    private void loadPieChartData() {
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colours = new ArrayList<>();
        for (int colour : ColorTemplate.MATERIAL_COLORS)
            colours.add(colour);
        for (int colour : ColorTemplate.VORDIPLOM_COLORS)
            colours.add(colour);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    // Check if dataSnapshot is not 'counter' since it is the only child
                    // that is not of class QuestionSet
                    if (!Objects.equals(dataSnapshot.getKey(), "counter")) {
                        QuestionSet questionSet = dataSnapshot.getValue(QuestionSet.class);
                        assert questionSet != null;
                        if (questionSet.getComplete()) {
                            entries.add(new PieEntry(questionSet.getWeight(), questionSet.getQuestionArray(0).getQuestion()));
                        }
                    }
                }
                PieDataSet dataSet = new PieDataSet(entries, "Expense Category");
                dataSet.setColors(colours);

                PieData data = new PieData(dataSet);
                data.setDrawValues(true);
                data.setValueFormatter(new PercentFormatter(pieChart));
                data.setValueTextSize(12f);
                data.setValueTextColor(Color.BLACK);

                pieChart.setData(data);
                pieChart.invalidate();

                pieChart.animateY(1400, Easing.EaseInOutQuad);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // Return to previous Activity
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}