package com.scottbezek.wingspan.sample;

import android.app.Activity;
import android.os.Bundle;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.scottbezek.wingspan.WingSpan;
import com.scottbezek.wingspan.WingSpan.ClickableSpanFactory;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Spanned foo = WingSpan.from("This is a <clickable>clickable test</clickable>")
                .bind("clickable", new ClickableSpanFactory() {
                    @Override
                    protected void onClick(View widget) {
                        Toast.makeText(widget.getContext(), "Hello World!", Toast.LENGTH_SHORT)
                                .show();
                    }
                })
                .buildSpanned();

        TextView tv = (TextView)findViewById(R.id.demo_text);
        tv.setText(foo);
        tv.setMovementMethod(new LinkMovementMethod());
    }

}
