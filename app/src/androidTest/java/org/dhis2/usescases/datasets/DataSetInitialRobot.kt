package org.dhis2.usescases.datasets

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.dhis2.R
import org.dhis2.common.BaseRobot
import org.dhis2.usescases.datasets.datasetInitial.TYPE_VALUES_BUTTON_TAG
import org.hamcrest.CoreMatchers.not

internal fun dataSetInitialRobot(
    composeTestRule: ComposeContentTestRule,
    dataSetInitialRobot: DataSetInitialRobot.() -> Unit) {
    DataSetInitialRobot(composeTestRule).apply {
        dataSetInitialRobot()
    }
}

internal class DataSetInitialRobot(
    private val composeTestRule: ComposeContentTestRule
) : BaseRobot() {

    fun clickOnInputOrgUnit() {
        waitForView(withId(R.id.dataSetOrgUnitInputLayout)).perform(click())
    }

    fun clickOnInputPeriod() {
        waitForView(withId(R.id.dataSetPeriodInputLayout)).perform(click())
    }

    fun clickOnActionButton() {
        composeTestRule.onNodeWithTag(TYPE_VALUES_BUTTON_TAG).performClick()
    }

    fun clickOnInputCatCombo() {
        waitForView(withId(R.id.input_layout)).perform(click())
    }

    fun selectCatCombo(catCombo: String) {
        waitForView(withText(catCombo)).perform(click())
    }

    fun chooseDate(date: String) {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("DATE_PICKER").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(
            label = "text",
            substring = true,
            useUnmergedTree = true,
        ).performClick()
        composeTestRule.onNodeWithContentDescription("Date", substring = true).performTextReplacement(date)
        composeTestRule.onNodeWithText("Accept", true).performClick()
        composeTestRule.waitForIdle()
    }

    fun checkActionInputIsNotDisplayed() {
        waitForView(withId(R.id.actionButtons)).check(matches(not(isDisplayed())))
    }

    fun checkActionInputIsDisplayed() {
        waitForView(withId(R.id.actionButtons)).check(matches(isDisplayed()))
    }
}
