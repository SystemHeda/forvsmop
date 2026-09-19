package ir.alamoot.plus;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private PlayerFragment playerFragment;
    private ListView deviceList;
    private ArrayAdapter<String> listAdapter;
    private OnvifDiscovery discovery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerFragment = (PlayerFragment) getSupportFragmentManager().findFragmentById(R.id.player_fragment);

        BrandPresets presets = new BrandPresets(this);
        Spinner brandSpinner = findViewById(R.id.brand_spinner);
        ArrayAdapter<String> sa = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, presets.names());
        sa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        brandSpinner.setAdapter(sa);

        EditText urlEdit = findViewById(R.id.url_edit);
        Button playBtn = findViewById(R.id.play_btn);
        Button discoverBtn = findViewById(R.id.discover_btn);
        deviceList = findViewById(R.id.device_list);
        TextView status = findViewById(R.id.status_text);
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceList.setAdapter(listAdapter);

        brandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                BrandPresets.Preset pr = presets.get(pos);
                urlEdit.setText(pr.sampleUrl);
                status.setText(pr.name + " | port=" + pr.httpPort + " | path=" + pr.streamPath);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        playBtn.setOnClickListener(v -> {
            String url = urlEdit.getText().toString().trim();
            if (!url.isEmpty()) playerFragment.play(url);
            else Toast.makeText(this, R.string.enter_url, Toast.LENGTH_SHORT).show();
        });

        discoverBtn.setOnClickListener(v -> {
            listAdapter.clear();
            status.setText(R.string.discovering);
            discovery = new OnvifDiscovery(this, (name, host) -> runOnUiThread(() -> {
                listAdapter.add(name + " — " + host);
                status.setText(getString(R.string.found_devices) + ": " + listAdapter.getCount());
            }));
            discovery.start(3000);
        });

        deviceList.setOnItemClickListener((p, v, pos, id) -> {
            String item = listAdapter.getItem(pos);
            String host = item.substring(item.lastIndexOf("— ") + 2).trim();
            BrandPresets.Preset pr = presets.get(brandSpinner.getSelectedItemPosition());
            playerFragment.play(pr.rtspUrl(host));
        });

        // Local web server for remote control / status page
        LocalWebServer.start(this, 8080, url -> runOnUiThread(() -> playerFragment.play(url)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (discovery != null) discovery.stop();
        LocalWebServer.stop();
    }
}
