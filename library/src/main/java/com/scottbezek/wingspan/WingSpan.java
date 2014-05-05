package com.scottbezek.wingspan;

import org.xml.sax.XMLReader;

import android.text.Editable;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Html.TagHandler;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.View;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Makes attaching spans to HTML-tagged Strings easier, with a simple fluent interface. <p> Simply
 * bind a {@link com.scottbezek.wingspan.WingSpan.SpanFactory} for each HTML tag type you'd like
 * to convert to spans. This will also convert basic HTML formatting tags to corresponding styled
 * spans - see {@link Html#fromHtml(String)} for more details on which standard tags are supported.
 * </p>
 */
public class WingSpan {

    private final String mSource;

    private final Map<String, SpanFactory> mFactories = new HashMap<String, SpanFactory>();

    private final SpanFactoryTagHandler mTagHandler = new SpanFactoryTagHandler(mFactories);

    private ImageGetter mImageGetter = null;

    private WingSpan(String source) {
        mSource = source;
    }

    /**
     * Construct a {@link com.scottbezek.wingspan.WingSpan} from an HTML String source. Any
     * substitutions/formatting-args should have already been made if desired.
     *
     * @return <code>this</code> for chaining calls.
     */
    public static WingSpan from(String source) {
        return new WingSpan(source);
    }

    /**
     * Set the {@link android.text.Html.ImageGetter} to be used for img tags.
     *
     * @return <code>this</code> for chaining calls.
     */
    public WingSpan setImageGetter(ImageGetter imageGetter) {
        mImageGetter = imageGetter;
        return this;
    }

    /**
     * Get the "canonical" tag name for a given tag.
     */
    private static String getCanonical(String tag) {
        return tag.toLowerCase(Locale.US);
    }

    /**
     * Bind a {@link com.scottbezek.wingspan.WingSpan.SpanFactory} to be used to construct spans for
     * HTML tags of the specified type.
     *
     * @param tag         The HTML tag, e.g. "blink" for <code>&lt;blink&gt;</code> tags. Must not
     *                    already be handled by {@link android.text.Html#fromHtml(String)}.
     * @param spanFactory Used to construct a span for each HTML tag of type <code>tag</code>
     *                    encountered.
     * @return <code>this</code> for chaining calls.
     */
    public WingSpan bind(String tag, SpanFactory spanFactory) {
        tag = getCanonical(tag);
        if (mFactories.containsKey(tag)) {
            throw new IllegalStateException("Factory already bound to tag " + tag);
        }
        mFactories.put(tag, spanFactory);
        return this;
    }

    /**
     * Build a spanned string from the original source.
     */
    public Spanned buildSpanned() {
        return Html.fromHtml(mSource, mImageGetter, mTagHandler);
    }

    /**
     * Builds Spans to be attached to the source String being processed.
     */
    public interface SpanFactory {

        /**
         * Construct and return a new span (such as a {@link android.text.style.StyleSpan}, {@link
         * android.text.style.ImageSpan}, etc) to be attached to the String being processed. Will
         * be attached to the String with flag {@link Spannable#SPAN_EXCLUSIVE_EXCLUSIVE}.
         *
         * @param tag           The HTML tag (all lower-case, e.g. "blink") being processed.
         * @param innerContents The contents that of the region of text that will be spanned.
         */
        @Nonnull
        Object getSpan(String tag, CharSequence innerContents);
    }

    /**
     * Helper {@link com.scottbezek.wingspan.WingSpan.SpanFactory} for constructing {@link
     * android.text.style.ClickableSpan}s. To use, subclass and implement the {@link
     * #onClick(android.view.View)}} method.
     */
    public abstract static class ClickableSpanFactory implements SpanFactory {

        @Override
        @Nonnull
        public Object getSpan(String tag, CharSequence innerContents) {
            return new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    ClickableSpanFactory.this.onClick(widget);
                }
            };
        }

        /**
         * Called when the span is clicked.
         */
        protected abstract void onClick(View widget);
    }

    private static class SpanFactoryTagHandler implements TagHandler {

        private final Map<String, SpanFactory> mFactories;

        public SpanFactoryTagHandler(Map<String, SpanFactory> factories) {
            mFactories = factories;
        }

        @Override
        public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
            tag = getCanonical(tag);
            SpanFactory factory = mFactories.get(tag);
            if (factory == null) {
                if ("html".equals(tag) || "body".equals(tag)) {
                    return;
                } else {
                    throw new IllegalStateException("No span factory bound for tag " + tag);
                }
            }
            if (opening) {
                int start = output.length();
                output.setSpan(new TagMarker(tag), start, start, Spannable.SPAN_MARK_MARK);
            } else {
                TagMarker startTag = getLast(output, tag);
                if (startTag == null) {
                    throw new IllegalStateException("No start tag found for tag " + tag);
                }
                int start = output.getSpanStart(startTag);
                if (start == -1) {
                    throw new IllegalStateException("Start tag found but not attached for tag "
                            + tag);
                }
                int end = output.length();
                output.removeSpan(startTag);
                if (start != end) {
                    output.setSpan(
                            factory.getSpan(tag, output.subSequence(start, end)),
                            start,
                            end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        private static TagMarker getLast(Spanned text, String tag) {
            Object[] spans = text.getSpans(0, text.length(), TagMarker.class);
            if (spans.length == 0) {
                return null;
            } else {
                for (int i = spans.length - 1; i >= 0; i--) {
                    TagMarker current = (TagMarker) spans[i];
                    if (current.tag.equals(tag)) {
                        return current;
                    }
                }
                return null;
            }
        }

        private static class TagMarker {

            public final String tag;

            public TagMarker(String tag) {
                this.tag = tag;
            }
        }
    }
}
