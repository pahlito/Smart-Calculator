package calculator

import java.math.BigInteger

const val END_MESSAGE = "Bye!"
const val NO_OUTPUT  = ""
const val ADD_OPERATOR = "+"
const val SUBTRACT_OPERATOR = "-"
const val MULTIPLY_OPERATOR = "*"
const val DIVIDE_OPERATOR = "/"
const val ADD_OPERATOR_A = "++"
const val ADD_OPERATOR_B = "--"
const val SUBTRACT_OPERATOR_A = "-+"
const val SUBTRACT_OPERATOR_B = "+-"
const val LEFT_PARENTHESES = "("
const val RIGHT_PARENTHESES = ")"

const val IS_COMMAND = "/[a-z]+"
const val IS_SPACE = "\\s*"
const val IS_OPERATOR = "(([-+]+)|[*/])"
const val IS_ASSIGN_OPERATOR = "\\s*=\\s*"
const val IS_VARIABLE = "[a-zA-Z]+"
const val IS_NUMBER = "[+-]?[0-9]+"
const val IS_VALUE = "($IS_VARIABLE|$IS_NUMBER)"
const val IS_ASSIGN_EXPRESSION = "$IS_SPACE$IS_VARIABLE$IS_ASSIGN_OPERATOR$IS_VALUE$IS_SPACE"
const val IS_PARENTHESES = "[\\(\\)]"
const val IS_MATH_EXPRESSION = "($IS_SPACE$IS_PARENTHESES?)*$IS_VALUE($IS_SPACE$IS_OPERATOR($IS_SPACE$IS_PARENTHESES?)*$IS_VALUE($IS_SPACE$IS_PARENTHESES?)*)*\\s*"

const val FIND_EXPRESSION_MEMBERS = "($IS_VARIABLE)|($IS_NUMBER)|($IS_OPERATOR)|($IS_PARENTHESES)"

val OPERATOR_PRIORITY = mapOf( "+" to 1, "-" to 1, "*" to 2, "/" to 2, "(" to 0)

val valuesPerName = mutableMapOf<String, BigInteger>()

fun main() {
    var total = NO_OUTPUT
    while (total != END_MESSAGE) {
        total = try {
            getInputValue()
        }catch(e: RuntimeException) {
            e.message!!
        }
        if (total != NO_OUTPUT) {
            println(total)
        }
    }
}

fun getInputValue(): String {
    val input = readln()
    return when  {
        isNullOrEmpty(input) -> NO_OUTPUT
        isCommand(input) -> evaluateCommand(input)
        isAssign(input) -> evaluateAssign(input)
        isMathematicalExpression(input) -> evaluateExpression(input)
        else -> "Invalid expression"
    }
}

fun evaluateCommand(command: String) = when (command) {
    "/exit" -> {
        END_MESSAGE
    }
    "/help" -> {
        "The program calculates additions and subtractions"
    }
    else -> {
        "Unknown command"
    }
}

fun evaluateAssign(assign: String): String{
    val (name, strValue) = assign.split(IS_ASSIGN_OPERATOR.toRegex())
    val value = strValue.trim()
    valuesPerName[name.trim()] = when {
        isNumber(value) -> strToBigInteger(value)
        else -> variableValue(value)
    }
    return NO_OUTPUT
}

fun evaluateExpression(input: String): String {
    val expressionMembers = FIND_EXPRESSION_MEMBERS.toRegex().findAll(input).map { it.value }.toList()
    return "${calculatePostFix(toPostFixList(expressionMembers))}"
}

fun toPostFixList(inputList: List<String>): List<String> {
    val result = mutableListOf<String>()
    val stack = ArrayDeque<String>()
    var previousWasValue = false
    for(input in inputList){
        if(previousWasValue && isNumber(input)){
            addToPostFix(input.substring(0, 1), result, stack, true)
            addToPostFix(input.substring(1), result, stack, false)
        } else {
            previousWasValue = addToPostFix(input, result, stack, previousWasValue)
        }
    }
    if(stack.contains(LEFT_PARENTHESES)){
        throw RuntimeException("Invalid expression")
    }
    result.addAll(stack)
    return result
}

private fun addToPostFix(
    input: String,
    result: MutableList<String>,
    stack: ArrayDeque<String>,
    previousWasValue: Boolean
): Boolean {
    if (isVariable(input) && input !in valuesPerName) {
        throw RuntimeException("Unknown variable")
    }
    when {
        isValue(input) -> {
            result.add(input)
            return true;
        }
        LEFT_PARENTHESES == input -> stack.addFirst(input)
        RIGHT_PARENTHESES == input -> addParenthesesOperators(result, stack)
        else -> {
            addOperator(cleanOperation(input), stack, result)
            return false
        }
    }
    return previousWasValue
}

fun addParenthesesOperators(result: MutableList<String>, stack: ArrayDeque<String>) {
    var founded = false
    while (!founded && !stack.isEmpty()){
        val operator = stack.removeFirst()
        founded = "(" == operator
        if(!founded){
            result.add(operator)
        }
    }
    if(!founded){
        throw RuntimeException("Invalid expression")
    }
}

fun addOperator(
    inputOperator: String,
    stack: ArrayDeque<String>,
    result: MutableList<String>
) = if (hasHigherPriority(inputOperator, stack)) {
    stack.addFirst(inputOperator)
} else {
    addHigherPriorityOperators(inputOperator, result, stack)
}

fun addHigherPriorityOperators(inputOperator: String,
                               result: MutableList<String>,
                               stack: ArrayDeque<String>) {
    do {
        result.add(stack.removeFirst())
    } while (!hasHigherPriority(inputOperator, stack))
    stack.addFirst(inputOperator)
}

fun hasHigherPriority(operator: String, stack: ArrayDeque<String>) = stack.isEmpty() || OPERATOR_PRIORITY[operator]!! > OPERATOR_PRIORITY[stack.first()]!!

fun calculatePostFix(expression: List<String>): BigInteger{
    val stack = ArrayDeque<BigInteger>()
    for ( member in expression ){
        when {
            isNumber(member) -> stack.addFirst(strToBigInteger(member))
            isVariable(member) -> stack.addFirst(variableValue(member))
            else -> stack.addFirst(operate(stack.removeFirst(), member, stack.removeFirst()))
        }
    }
    return stack.first()
}

fun variableValue(member: String) = if (member in valuesPerName){
    valuesPerName[member]!!
}else {
    throw RuntimeException("Unknown variable")
}

fun strToBigInteger(strNumber: String) = try {
    strNumber.toBigInteger()
} catch (e: NumberFormatException) {
    BigInteger.ZERO
}

fun operate(number: BigInteger, operator: String, total: BigInteger): BigInteger = when (operator) {
    MULTIPLY_OPERATOR -> total * number
    DIVIDE_OPERATOR -> total / number
    SUBTRACT_OPERATOR -> total - number
    else -> total + number
}

fun cleanOperation(inputItem: String): String {
    var item = inputItem
    while ( containsDoubleOperator(item) ) {
        item = item.replace(ADD_OPERATOR_A, "$ADD_OPERATOR")
        item = item.replace(ADD_OPERATOR_B, "$ADD_OPERATOR")
        item = item.replace(SUBTRACT_OPERATOR_A, "$SUBTRACT_OPERATOR")
        item = item.replace(SUBTRACT_OPERATOR_B, "$SUBTRACT_OPERATOR")
    }
    return item
}

fun containsDoubleOperator(item: String): Boolean = item.contains(ADD_OPERATOR_A) ||
        item.contains(ADD_OPERATOR_B) || item.contains(SUBTRACT_OPERATOR_A) || item.contains(SUBTRACT_OPERATOR_B)

fun isNullOrEmpty(str: String) = null == str || str.isEmpty()

fun isCommand(strCommand: String) = IS_COMMAND.toRegex().matches(strCommand)

fun isAssign(strAssign: String) = IS_ASSIGN_EXPRESSION.toRegex().matches(strAssign)

fun isMathematicalExpression(strMathExpr: String) = IS_MATH_EXPRESSION.toRegex().matches(strMathExpr)

fun isValue(strValue: String) = IS_VALUE.toRegex().matches(strValue)

fun isVariable(strVariable: String) = IS_VARIABLE.toRegex().matches(strVariable)

fun isNumber(strNumber: String) = IS_NUMBER.toRegex().matches(strNumber)

fun isOperator(strOperator: String) = IS_OPERATOR.toRegex().matches(strOperator)

