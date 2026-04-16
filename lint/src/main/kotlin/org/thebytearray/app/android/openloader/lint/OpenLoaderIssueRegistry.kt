package org.thebytearray.app.android.openloader.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue


class OpenLoaderIssueRegistry : IssueRegistry() {

    override val issues: List<Issue> = emptyList()

    override val api: Int = CURRENT_API

    override val minApi: Int = 12

    override val vendor: Vendor = Vendor(
        vendorName = "OpenLoader",
        feedbackUrl = "https://github.com/thebytearray/openloader/issues",
        contact = "https://github.com/thebytearray/openloader",
    )
}
