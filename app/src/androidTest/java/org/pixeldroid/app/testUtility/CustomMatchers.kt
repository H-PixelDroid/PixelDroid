package org.pixeldroid.app.testUtility

import android.text.SpannableString
import android.text.style.ClickableSpan
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.*
import androidx.test.espresso.action.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.util.TreeIterables
import org.pixeldroid.app.R
import org.hamcrest.BaseMatcher
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import java.util.concurrent.TimeoutException


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
annotation class RepeatTest(val value: Int = 1)

class RepeatRule : TestRule {

    private class RepeatStatement(private val statement: Statement, private val repeat: Int) : Statement() {
        @Throws(Throwable::class)
        override fun evaluate() {
            for (i in 0 until repeat) {
                statement.evaluate()
            }
        }
    }

    override fun apply(statement: Statement, description: org.junit.runner.Description): Statement {
        var result = statement
        val repeat = description.getAnnotation(RepeatTest::class.java)
        if (repeat != null) {
            val times = repeat.value
            result = RepeatStatement(statement, times)
        }
        return result
    }
}

fun ViewInteraction.isDisplayed(): Boolean {
    return try {
        check(matches(ViewMatchers.isDisplayed()))
        true
    } catch (e: NoMatchingViewException) {
        false
    }
}

/**
 * Waits for a view to appear in the hierarchy
 * Doesn't work if the root changes (since it operates on the root!)
 * @param viewId The id of the view to wait for.
 */
fun waitForView(viewId: Int) {
    Espresso.onView(isRoot()).perform(waitForViewViewAction(viewId))
}

/**
 * This ViewAction tells espresso to wait till a certain view is found in the view hierarchy.
 * @param viewId The id of the view to wait for.
 */
private fun waitForViewViewAction(viewId: Int): ViewAction {
    // The maximum time which espresso will wait for the view to show up (in milliseconds)
    val timeOut = 5000
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isRoot()
        }

        override fun getDescription(): String {
            return "wait for a specific view with id $viewId; during $timeOut millis."
        }

        override fun perform(uiController: UiController, rootView: View) {
            uiController.loopMainThreadUntilIdle()
            val startTime = System.currentTimeMillis()
            val endTime = startTime + timeOut
            val viewMatcher = withId(viewId)

            do {
                // Iterate through all views on the screen and see if the view we are looking for is there already
                for (child in TreeIterables.breadthFirstViewTraversal(rootView)) {
                    // found view with required ID
                    if (viewMatcher.matches(child)) {
                        return
                    }
                }
                // Loops the main thread for a specified period of time.
                // Control may not return immediately, instead it'll return after the provided delay has passed and the queue is in an idle state again.
                uiController.loopMainThreadForAtLeast(100)
            } while (System.currentTimeMillis() < endTime) // in case of a timeout we throw an exception -&gt; test fails
            throw PerformException.Builder()
                    .withCause(TimeoutException())
                    .withActionDescription(this.description)
                    .withViewDescription(HumanReadables.describe(rootView))
                    .build()
        }
    }
}

fun clickClickableSpanInDescription(textToClick: CharSequence): ViewAction {
    return object : ViewAction {

        override fun getConstraints(): Matcher<View> {
            return Matchers.instanceOf(TextView::class.java)
        }

        override fun getDescription(): String {
            return "clicking on a ClickableSpan"
        }

        override fun perform(uiController: UiController, view: View) {
            val textView = view.findViewById<View>(R.id.description) as TextView
            val spannableString = SpannableString(textView.text)

            if (spannableString.isEmpty()) {
                // TextView is empty, nothing to do
                throw NoMatchingViewException.Builder()
                        .includeViewHierarchy(true)
                        .withRootView(textView)
                        .build()
            }

            // Get the links inside the TextView and check if we find textToClick
            val spans = spannableString.getSpans(0, spannableString.length, ClickableSpan::class.java)
            if (spans.isNotEmpty()) {
                var spanCandidate: ClickableSpan
                for (span: ClickableSpan in spans) {
                    spanCandidate = span
                    val start = spannableString.getSpanStart(spanCandidate)
                    val end = spannableString.getSpanEnd(spanCandidate)
                    val sequence = spannableString.subSequence(start, end)
                    if (textToClick.toString() == sequence.toString()) {
                        span.onClick(textView)
                        return
                    }
                }
            }

            // textToClick not found in TextView
            throw NoMatchingViewException.Builder()
                    .includeViewHierarchy(true)
                    .withRootView(textView)
                    .build()

        }
    }
}

fun typeSearchViewText(text: String): ViewAction {
    return object : ViewAction {
        override fun getDescription(): String {
            return "Change view text"
        }

        override fun getConstraints(): Matcher<View> {
            return allOf(isDisplayed(), isAssignableFrom(SearchView::class.java))
        }

        override fun perform(uiController: UiController?, view: View?) {
            (view as SearchView).setQuery(text, true)
        }
    }
}

fun <T> first(matcher: Matcher<T>): Matcher<T> {
    return object : BaseMatcher<T>() {
        var isFirst = true
        override fun describeTo(description: org.hamcrest.Description?) {
            description?.appendText("first matching item")
        }

        override fun matches(item: Any?): Boolean {
            if (isFirst && matcher.matches(item)) {
                isFirst = false
                return true
            }
            return false
        }

    }
}

fun <T> second(matcher: Matcher<T>): Matcher<T> {
    return object : BaseMatcher<T>() {
        var isFirst = true
        override fun describeTo(description: org.hamcrest.Description?) {
            description?.appendText("second matching item")
        }

        override fun matches(item: Any?): Boolean {
            if (isFirst && matcher.matches(item)) {
                isFirst = false
                return false
            } else if (!isFirst && matcher.matches(item))
                return true
            return false
        }

    }
}


fun atPosition(position: Int, itemMatcher: Matcher<View?>): Matcher<View?> {
    return object : BoundedMatcher<View?, RecyclerView>(RecyclerView::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("has item at position $position: ")
            itemMatcher.describeTo(description)
        }

        override fun matchesSafely(view: RecyclerView): Boolean {
            val viewHolder = view.findViewHolderForAdapterPosition(position)
                    ?: // has no item on such position
                    return false
            return itemMatcher.matches(viewHolder.itemView)
        }
    }
}


/**
 * @param percent can be 1 or 0
 * 1: swipes all the way up
 * 0: swipes half way up
 */
fun slowSwipeUp(percent: Boolean): ViewAction {
    return ViewActions.actionWithAssertions(
            GeneralSwipeAction(
                    Swipe.SLOW,
                    GeneralLocation.BOTTOM_CENTER,
                    if (percent) GeneralLocation.TOP_CENTER else GeneralLocation.CENTER,
                    Press.FINGER
            )
    )
}

/**
 * @param percent can be 1 or 0
 * 1: swipes all the way left
 * 0: swipes half way left
 */
fun slowSwipeLeft(percent: Boolean): ViewAction {
    return ViewActions.actionWithAssertions(
            GeneralSwipeAction(
                    Swipe.SLOW,
                    GeneralLocation.CENTER_RIGHT,
                    if (percent) GeneralLocation.CENTER_LEFT else GeneralLocation.CENTER,
                    Press.FINGER
            )
    )
}

fun getText(matcher: Matcher<View?>?): String? {
    val stringHolder = arrayOf<String?>(null)
    Espresso.onView(matcher).perform(object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return ViewMatchers.isAssignableFrom(TextView::class.java)
        }

        override fun getDescription(): String {
            return "getting text from a TextView"
        }

        override fun perform(
                uiController: UiController,
                view: View,
        ) {
            val tv = view as TextView //Save, because of check in getConstraints()
            stringHolder[0] = tv.text.toString()
        }
    })
    return stringHolder[0]
}

fun clickChildViewWithId(id: Int) = object : ViewAction {

    override fun getConstraints() = null

    override fun getDescription() = "click child view with id $id"

    override fun perform(uiController: UiController, view: View) {
        val v = view.findViewById<View>(id)
        v.performClick()
    }
}

fun typeTextInViewWithId(id: Int, text: String) = object : ViewAction {

    override fun getConstraints() = null

    override fun getDescription() = "click child view with id $id"

    override fun perform(uiController: UiController, view: View) {
        val v = view.findViewById<EditText>(id)
        v.text.append(text)
    }
}