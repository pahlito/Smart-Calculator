fun summator(map: Map<Int, Int>): Int {
    var total = 0
    for((key, value) in map) {
        total += if (key % 2 == 0) value else 0
    }
    return total
}