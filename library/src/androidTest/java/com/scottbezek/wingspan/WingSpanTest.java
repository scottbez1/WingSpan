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

    public void testNested() {
        final Object spanInner = new Object();
        final Object spanOuter = new Object();

        Spanned result = WingSpan
                .from("abc: <outer>testing that <inner>nested tags</inner> work</outer> as expected")
                .bind("outer", new SpanFactory() {
                    @Nonnull
                    @Override
                    public Object getSpan(String tag, CharSequence innerContents) {
                        assertEquals("testing that nested tags work", innerContents.toString());
                        Spanned spanned = ((Spanned)innerContents);
                        Object[] spans = spanned.getSpans(0, innerContents.length(), Object.class);
                        assertEquals(1, spans.length);
                        assertSame(spanInner, spans[0]);
                        return spanOuter;
                    }
                })
                .bind("inner", new SpanFactory() {
                    @Nonnull
                    @Override
                    public Object getSpan(String tag, CharSequence innerContents) {
                        assertEquals("nested tags", innerContents.toString());
                        return spanInner;
                    }
                })
                .buildSpanned();

        assertEquals("abc: testing that nested tags work as expected", result.toString());
        Object[] spans = result.getSpans(0, result.length(), Object.class);
        assertEquals(2, spans.length);
        assertEquals(5, result.getSpanStart(spanOuter));
        assertEquals(34, result.getSpanEnd(spanOuter));
        assertEquals(18, result.getSpanStart(spanInner));
        assertEquals(29, result.getSpanEnd(spanInner));
    }

    /**
     * This tests the crazy loose parsing behavior of TagSoup (used internally by Html.fromHtml).
     * This test is not intended to make any sense on its own - it mostly serves to document
     * and verify the crazy behavior of TagSoup.
     *
     * See https://code.google.com/p/android/issues/detail?id=69388
     */
    public void testAbsurdTagSoupParsingBehavior() {
        class HtmlSpan extends Object {};
        class BodySpan extends Object {};
        class CustomSpan extends Object {};

        SpanFactory customSpanFactory = new SpanFactory() {
            @Nonnull
            @Override
            public Object getSpan(String tag, CharSequence innerContents) {
                return new CustomSpan();
            }
        };
        SpanFactory htmlSpanFactory = new SpanFactory() {
            @Nonnull
            @Override
            public Object getSpan(String tag, CharSequence innerContents) {
                return new HtmlSpan();
            }
        };
        SpanFactory bodySpanFactory = new SpanFactory() {
            @Nonnull
            @Override
            public Object getSpan(String tag, CharSequence innerContents) {
                return new BodySpan();
            }
        };

        String stringA = "<custom>inside the tag</custom> outside the tag";
        String stringB = "<script>inside the tag</script> outside the tag";

        {
            Spanned resultA = WingSpan
                    .from(stringA)
                    .bind("html", htmlSpanFactory)
                    .bind("body", bodySpanFactory)
                    .bind("custom", customSpanFactory)
                    .buildSpanned();
            assertEquals("inside the tag outside the tag", resultA.toString());
            Object[] htmlSpans = resultA.getSpans(0, resultA.length(), HtmlSpan.class);
            Object[] bodySpans = resultA.getSpans(0, resultA.length(), BodySpan.class);
            Object[] customSpans = resultA.getSpans(0, resultA.length(), CustomSpan.class);
            assertEquals(0, htmlSpans.length);
            assertEquals(0, bodySpans.length);
            assertEquals(1, customSpans.length);
            assertEquals(0, resultA.getSpanStart(customSpans[0]));
            assertEquals(30, resultA.getSpanEnd(customSpans[0]));
        }

        {
            Spanned resultB = WingSpan
                    .from(stringB)
                    .bind("html", htmlSpanFactory)
                    .bind("body", bodySpanFactory)
                    .bind("script", customSpanFactory)
                    .buildSpanned();
            assertEquals("inside the tag outside the tag", resultB.toString());
            Object[] htmlSpans = resultB.getSpans(0, resultB.length(), HtmlSpan.class);
            Object[] bodySpans = resultB.getSpans(0, resultB.length(), BodySpan.class);
            Object[] customSpans = resultB.getSpans(0, resultB.length(), CustomSpan.class);
            assertEquals(1, htmlSpans.length);
            assertEquals(0, resultB.getSpanStart(htmlSpans[0]));
            assertEquals(30, resultB.getSpanEnd(htmlSpans[0]));
            assertEquals(1, bodySpans.length);
            assertEquals(14, resultB.getSpanStart(bodySpans[0]));
            assertEquals(30, resultB.getSpanEnd(bodySpans[0]));
            assertEquals(1, customSpans.length);
            assertEquals(0, resultB.getSpanStart(customSpans[0]));
            assertEquals(14, resultB.getSpanEnd(customSpans[0]));
        }

    }
}
