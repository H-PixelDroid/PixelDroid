package org.pixeldroid.app.testUtility;

import android.content.res.Resources;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

import static org.hamcrest.CoreMatchers.is;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.test.espresso.matcher.BoundedMatcher;

public class ToolbarMatchers {

    public static Matcher<View> withToolbarTitle(CharSequence text) {
        return new WithCharSequenceMatcher(is(text), ToolbarMethod.GET_TITLE);
    }

    public static Matcher<View> withToolbarSubtitle(CharSequence text) {
        return new WithCharSequenceMatcher(is(text), ToolbarMethod.GET_SUBTITLE);
    }


    public static Matcher<View> withToolbarTitle(final int resourceId) {
        return new WithStringResourceMatcher(resourceId, ToolbarMethod.GET_TITLE);
    }

    public static Matcher<View> withToolbarSubTitle(final int resourceId) {
        return new WithStringResourceMatcher(resourceId, ToolbarMethod.GET_SUBTITLE);
    }


    private static final class WithCharSequenceMatcher extends BoundedMatcher<View, Toolbar> {

        private final Matcher<CharSequence> charSequenceMatcher;
        private final ToolbarMethod method;

        private WithCharSequenceMatcher(Matcher<CharSequence> charSequenceMatcher, ToolbarMethod method) {
            super(Toolbar.class);
            this.charSequenceMatcher = charSequenceMatcher;
            this.method = method;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("with text: ");
            charSequenceMatcher.describeTo(description);
        }

        @Override
        protected boolean matchesSafely(Toolbar toolbar) {

            CharSequence actualText;
            switch (method) {
                case GET_TITLE:
                    actualText = toolbar.getTitle();
                    break;
                case GET_SUBTITLE:
                    actualText = toolbar.getSubtitle();
                    break;
                default:
                    throw new IllegalStateException("Unexpected Toolbar method: " + method.toString());
            }

            return charSequenceMatcher.matches(actualText);
        }
    }

    static final class WithStringResourceMatcher extends BoundedMatcher<View, Toolbar> {

        private final int resourceId;

        @Nullable
        private String resourceName;
        @Nullable
        private String expectedText;

        private final ToolbarMethod method;


        private WithStringResourceMatcher(int resourceId, ToolbarMethod method) {
            super(Toolbar.class);
            this.resourceId = resourceId;
            this.method = method;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("with string from resource id: ").appendValue(resourceId);
            if (null != resourceName) {
                description.appendText("[").appendText(resourceName).appendText("]");
            }
            if (null != expectedText) {
                description.appendText(" value: ").appendText(expectedText);
            }
        }

        @Override
        public boolean matchesSafely(Toolbar toolbar) {
            if (null == expectedText) {
                try {
                    expectedText = toolbar.getResources().getString(resourceId);
                    resourceName = toolbar.getResources().getResourceEntryName(resourceId);
                } catch (Resources.NotFoundException ignored) {
                    /* view could be from a context unaware of the resource id. */
                }
            }
            CharSequence actualText;
            switch (method) {
                case GET_TITLE:
                    actualText = toolbar.getTitle();
                    break;
                case GET_SUBTITLE:
                    actualText = toolbar.getSubtitle();
                    break;
                default:
                    throw new IllegalStateException("Unexpected Toolbar method: " + method.toString());
            }
            // FYI: actualText may not be string ... its just a char sequence convert to string.
            return null != expectedText
                    && null != actualText
                    && expectedText.equals(actualText.toString());
        }
    }

    enum ToolbarMethod {
        GET_TITLE,
        GET_SUBTITLE
    }
}