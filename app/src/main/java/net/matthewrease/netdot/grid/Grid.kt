package net.matthewrease.netdot.grid

import java.util.*

/**
 * Simple Data Class for Managing Point-based Grids.
 * @param _size Grid dimensions.
 * @author Matthew Rease
 * @see GridPoint
 */
class Grid(
    private val _size: Dimension = Dimension(0, 0)) : Set<GridPoint> {
    companion object {
        /**
         * Convert string representation of grid dimensions into a Dimension object.
         * <br/>
         * Uses {#link Dimension}[Integer.parseInt], and [String.indexOf], and may throw exceptions if the string is not properly formatted.
         * @param grid Properly formatted grid dimensions
         * @return Grid of specified size
         * @see Grid.toString
         */
        fun parseDimension(grid: String): Dimension {
            val x = grid.indexOf('x')
            return if (x == -1)
                Dimension(-1, -1)
            else
                Dimension(
                    grid.substring(0, grid.indexOf('x')).toInt(),
                    grid.substring(grid.indexOf('x') + 1).toInt()
                )
        }
    }

    data class Dimension(var width: Int, var height: Int)

    private var _maxSpaces = 0
    private val points = HashSet<GridPoint>()

    /**
     * Vertical grid point count.
     */
    val height
        get() = _size.height
    /**
     * Number of square spaces between the points on the grid.
     */
    val maxSpaces
        get() = _maxSpaces
    /**
     * Square size of grid.
     */
    override val size: Int
        get() = _size.width * _size.height
    /**
     * Horizontal grid point count.
     */
    val width
        get() = _size.width

    /**
     * Check if a point is inside the grid.
     * @param element The point to check
     * @return `true` if this point is within the bounds of the grid, `false` otherwise
     */
    override fun contains(element: GridPoint): Boolean =
        element.x in 0 until _size.width && element.y in 0 until _size.height

    override fun containsAll(elements: Collection<GridPoint>): Boolean =
        elements.none { it !in this }

    /*inline fun forEach(action: (GridPoint?) -> Unit) {
        for (element in newArray()) action(element)
    }*/
    /*fun forEach(action: Consumer<GridPoint?>) {
        Objects.requireNonNull(action)
        for (point in newArray()) {
            action.accept(point)
        }
    }*/

    override fun isEmpty(): Boolean =
        _size.width < 1 || _size.height < 1

    override fun iterator(): MutableIterator<GridPoint> =
        points.iterator()

    /**
     * Create an array of all possible points in the grid.
     * @return Array of [GridPoint]s, one for every point on the grid
     */
    fun newArray(): Array<GridPoint?> {
        val array = arrayOfNulls<GridPoint>(_size.width * _size.height)
        for (x in 0 until _size.width) for (y in 0 until _size.height) array[_size.height * x + y] = GridPoint(x, y)
        return array
    }

    fun resize(size: Dimension) {
        when {
            // Remove old points
            size.width < _size.width -> points.removeAll { it.x >= size.width }
            // Add new points
            size.width > _size.width ->
                points.addAll(
                    (_size.width until size.width).toList()
                        .map { column ->
                            (0 until _size.height).toList()
                                .map { row ->
                                    GridPoint(column, row) } }
                        .reduce { a, b -> a + b }
                )
        }
        _size.width = size.width
        when {
            // Remove old points
            size.height < _size.height -> points.removeAll { it.y >= size.height }
            // Add new points
            size.height > _size.height ->
                points.addAll(
                    (_size.height until size.height).toList()
                        .map { row ->
                            (0 until _size.width).toList()
                                .map { column ->
                                    GridPoint(column, row) } }
                        .reduce { a, b -> a + b }
                )
        }
        _size.height = size.height
        _maxSpaces = (_size.width - 1) * (_size.height - 1)
    }

    fun resize(grid: Grid) =
        resize(Dimension(grid.width, grid.height))

    override fun toString(): String =
        "${_size.width}x${_size.height}"

    init {
        if (_size.width > 0 && _size.height > 0)
            for (column in 0 until _size.width)
                for (row in 0 until _size.height)
                    points.add(GridPoint(column, row))
    }
}
