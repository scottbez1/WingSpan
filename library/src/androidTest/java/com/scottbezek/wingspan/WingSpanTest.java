package com.scottbezek.wingspan;

import com.scottbezek.wingspan.WingSpan.SpanFactory;

import junit.framework.TestCase;

import android.test.MoreAsserts;
import android.text.Spanned;
import android.text.style.StyleSpan;

import java.util.Arrays;

import javax.annotation.Nonnull;

public class WingSpanTest extends TestCase {

    private static final SpanFactory OBJECT_SPAN_FACTORY = new SpanFactory() {
        @Nonnull
        @Override
        public Object getSpan(String tag, CharSequence innerContents) {
            return new Object();
        }
    };

    public void testSimple() {
        final StyleSpan expectedSpan = new StyleSpan(0);
        SpanFactory sf = new SpanFactory() {
            @Nonnull
            @Override
            public Object getSpan(String tag, CharSequence innerContents) {
                assertEquals("foobar", tag);
                assertEquals("testing", innerContents.toString());
                return expectedSpan;
            }
        };

        Spanned result = WingSpan.from("before<foobar>testing</foobar>after")
                .bind("foobar", sf)
                .buildSpanned();

        StyleSpan spans[] = result.getSpans(0, result.length(), StyleSpan.class);

        MoreAsserts.assertContentsInOrder(Arrays.asList(spans), expectedSpan);
        assertEquals(6, result.getSpanStart(spans[0]));
        assertEquals(13, result.getSpanEnd(spans[0]));
        assertEquals(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE, result.getSpanFlags(spans[0]));
    }

    public void testDoubleBindFails() {
        WingSpan w = WingSpan.from("string");

        w.bind("foobar", OBJECT_SPAN_FACTORY);
        try {
            w.bind("foobar", OBJECT_SPAN_FACTORY);
            fail();
        } catch (Exception expected) {};

        // Verify that non-canonical tag binding for a duplicate also fails
        try {
            w.bind("fOoBaR", OBJECT_SPAN_FACTORY);
            fail();
        } catch (Exception expected) {};
    }

    public void testUnboundFails() {
        WingSpan w = WingSpan.from("string<unbound>test</unbound>");

        try {
            w.buildSpanned();
            fail();
        } catch (Exception expected) {};
    }

    public void testBindReservedFails() {
        WingSpan w = WingSpan.from("string");

        try {
            w.bind("a", OBJECT_SPAN_FACTORY);
            fail();
        } catch (Exception expected) {};
        try {
            w.bind("br", OBJECT_SPAN_FACTORY);
            fail();
        } catch (Exception expected) {};
        try {
            w.bind("div", OBJECT_SPAN_FACTORY);
            fail();
        } catch (Exception expected) {};
        try {
            w.bind("strong", OBJECT_SPAN_FACTORY);
            fail();
        } catch (Exception expected) {};

        w.bind("notreserved", OBJECT_SPAN_FACTORY);
    }
}
