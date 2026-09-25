package app.yuki.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.ConstantEvaluator
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UPolyadicExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.skipParenthesizedExprDown

private val TEXT_PARAMETERS = setOf(
    "text",
    "contentDescription",
    "label",
    "title",
    "description",
    "supporting",
    "placeholder",
    "actionLabel",
    "onClickLabel",
)

private val SNACKBAR_PARAMETERS = setOf("message", "actionLabel")

private val UI_PACKAGES = listOf(
    "androidx.compose",
    "app.yuki.core.designsystem",
    "app.yuki.feature",
)

private const val ANIMATION_PACKAGE = "androidx.compose.animation"
private const val COMPOSABLE = "androidx.compose.runtime.Composable"
private const val SNACKBAR_METHOD = "showSnackbar"
private const val CONTENT_DESCRIPTION = "contentDescription"

class HardcodedComposeTextDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UCallExpression::class.java, UBinaryExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        HardcodedTextHandler(context)

    private class HardcodedTextHandler(private val context: JavaContext) : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            val method = node.resolve() ?: return
            if (!isUiApi(method) || isAnimation(method)) return

            val parameters = textParametersOf(method)
            val arguments = context.evaluator.computeArgumentMapping(node, method)

            arguments.forEach { (argument, parameter) ->
                if (parameter.name in parameters) reportIfHardcoded(argument)
            }
        }

        override fun visitBinaryExpression(node: UBinaryExpression) {
            if (node.operator != UastBinaryOperator.ASSIGN) return

            val target = node.leftOperand as? USimpleNameReferenceExpression ?: return
            if (target.identifier == CONTENT_DESCRIPTION) reportIfHardcoded(node.rightOperand)
        }

        private fun textParametersOf(method: PsiMethod): Set<String> =
            if (method.name == SNACKBAR_METHOD) SNACKBAR_PARAMETERS else TEXT_PARAMETERS

        private fun isUiApi(method: PsiMethod): Boolean {
            val owner = packageOf(method)
            val isUiPackage = UI_PACKAGES.any { prefix -> owner.startsWith(prefix) }

            return isUiPackage || method.hasAnnotation(COMPOSABLE)
        }

        private fun isAnimation(method: PsiMethod): Boolean =
            packageOf(method).startsWith(ANIMATION_PACKAGE)

        private fun packageOf(method: PsiMethod): String =
            context.evaluator.getPackage(method)?.qualifiedName.orEmpty()

        private fun reportIfHardcoded(expression: UExpression) {
            if (!isHardcoded(expression.skipParenthesizedExprDown())) return

            context.report(ISSUE, expression, context.getLocation(expression), MESSAGE)
        }
    }

    companion object {
        private const val MESSAGE = "User-facing text must come from a string resource"
        private const val EXPLANATION =
            "Text shown to users must come from string resources so it can be translated."

        val ISSUE: Issue = Issue.create(
            id = "HardcodedComposeText",
            briefDescription = "Hardcoded user-facing text",
            explanation = EXPLANATION,
            category = Category.I18N,
            priority = 6,
            severity = Severity.ERROR,
            implementation = Implementation(
                HardcodedComposeTextDetector::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}

private fun isHardcoded(expression: UExpression): Boolean = when (expression) {
    is ULiteralExpression -> isWording(expression.value)
    is UPolyadicExpression -> expression.operands.any(::isHardcoded)
    else -> isWording(ConstantEvaluator.evaluate(null, expression))
}

private fun isWording(value: Any?): Boolean = (value as? String)?.any(Char::isLetter) == true
