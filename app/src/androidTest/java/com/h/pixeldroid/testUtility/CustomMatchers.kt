package com.h.pixeldroid.testUtility

import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.*
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher


abstract class CustomMatchers {
    companion object {
        fun <T> first(matcher: Matcher<T>): Matcher<T>? {
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

        fun <T> second(matcher: Matcher<T>): Matcher<T>? {
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


        fun atPosition(position: Int, itemMatcher: Matcher<View?>): Matcher<View?>? {
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
        fun slowSwipeUp(percent: Boolean) : ViewAction {
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
        fun slowSwipeLeft(percent: Boolean) : ViewAction {
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
                    view: View
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
    }
}