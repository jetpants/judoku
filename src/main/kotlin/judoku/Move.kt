/*	Copyright (C) 2018 Steve Ball <jetpants@gmail.com>

	This file is part of Judoku. Judoku is free software: you can redistribute
	it and/or modify it under the terms of the GNU General Public License as
	published by the Free Software Foundation, either version 3 of the License,
	or (at your option) any later version.

    Judoku is distributed in the hope that it will be useful, but WITHOUT ANY
    WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
    FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
    details.

    You should have received a copy of the GNU General Public License along with
    Judoku. If not, see <http://www.gnu.org/licenses/>.
*/

package judoku

data class Move(
        // use getNth(), getValue(), getRule(), etc to access these members from Java
        val nth: Int,                   // the cell for which the move is suggested
        val value: Int,                 // the value it should be set to
        val rule: Rule,                 // the rule that suggested this move
        val explanation: String,        // explanation of the reasoning
        val others: IntArray) {         // the other cells (in nths) that led to this move
    enum class Rule {
        ONE_MISSING,            // group (column/row/box) has all values except one
        ONE_POSSIBILITY         // cell has only one possible value that it could take
    }
}
