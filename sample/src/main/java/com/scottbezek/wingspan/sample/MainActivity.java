package com.scottbezek.wingspan.sample;

import com.scottbezek.wingspan.WingSpan;
import com.scottbezek.wingspan.WingSpan.ClickableSpanFactory;
import com.scottbezek.wingspan.WingSpan.SpanFactory;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import javax.annotation.Nonnull;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LinearLayout root = (LinearLayout)findViewById(R.id.demo_root);

        TextView tv1 = new TextView(this);
        tv1.setText(WingSpan.from("This is a <clickable>clickable test</clickable>")
                .bind("clickable", new ClickableSpanFactory() {
                    @Override
                    protected void onClick(View widget) {
                        Toast.makeText(widget.getContext(), "Hello World!", Toast.LENGTH_SHORT)
                                .show();
                    }
                })
                .buildSpanned());
        tv1.setMovementMethod(new LinkMovementMethod());
        root.addView(tv1, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));


        TextView tv2 = new TextView(this);
        tv2.setText(WingSpan.from(
                "This is a <miniscule>miniscule element with <font color=\"#ff0000\">nested red text</font></miniscule>!!!")
                .bind("miniscule", new SpanFactory() {
                    @Nonnull
                    @Override
                    public Object getSpan(String tag, CharSequence innerContents) {
                        return new RelativeSizeSpan(0.5f);
                    }
                })
                .buildSpanned());
        root.addView(tv2, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

}
