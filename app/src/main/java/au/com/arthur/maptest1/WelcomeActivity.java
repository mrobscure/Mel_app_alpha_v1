package au.com.arthur.maptest1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.widget.Button;

public class WelcomeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button btn = (Button) findViewById(R.id.wbutton);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchMap(v);
            }
        });
    }

    public void launchMap(View view) {
        startActivity(new Intent(this, MapsActivity.class));
    }
}
