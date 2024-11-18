package org.example.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Game {

    // row length
    int row;

    // col length
    int col;

    // board content
    int[][] board;

    public Game(int[][] board){
        this.board = board;
        this.row = board.length;
        this.col = board[0].length;
    }

    // randomly initialize the game board
    public static int[][] SetupBoard(int row, int col) {
        // TODO: randomly initialize board
        Random random = new Random();
        int[][] board = new int[row][col];
        List<Integer> numbers = new ArrayList<>();
        while (!validBoard(board)){
            for (int i = 1; i <= row*col/2; i++) {
                int a = random.nextInt(10)+1;
                for (int j = 0; j < 2; j++) {
                    numbers.add(a);
                }
            }
            if (row*col%2!=0)numbers.add(random.nextInt(10)+1);
            Collections.shuffle(numbers);
            for (int r = 0; r < row; r++) {
                for (int c = 0; c < col; c++) {
                    board[r][c] = numbers.remove(0);
                }
            }
        }
        return board;
//        Random random = new Random();
//        int[][] board = new int[row][col];
//
//        // 随机填充棋盘
//        for (int r = 0; r < row; r++) {
//            for (int c = 0; c < col; c++) {
//                board[r][c] = random.nextInt(11)+1;
//            }
//        }
//
//        return board;
//        return new int[][]{
//                {0, 0, 0, 0, 0, 0},
//                {0, 1, 1, 2, 2, 0},
//                {0, 3, 3, 4, 4, 0},
//                {0, 5, 5, 6, 6, 0},
//                {0, 7, 7, 8, 8, 0},
//                {0, 0, 0, 0, 0, 0}
//        };
    }
    public static boolean validBoard(int[][] board) {
        int rows = board.length;
        int cols = board[0].length;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (col < cols - 1 && board[row][col] == board[row][col + 1] && board[row][col] != 0) {
                    return true;
                }
                if (row < rows - 1 && board[row][col] == board[row + 1][col] && board[row][col] != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    // judge the validity of an operation

    public boolean judge(int row1, int col1, int row2, int col2){
        if ((board[row1][col1] != board[row2][col2]) || (row1 == row2 && col1 == col2)) {
            return false;
        }

        // one line
        if (isDirectlyConnected(row1, col1, row2, col2, board)) {
            return true;
        }

        // two lines
        if((row1 != row2) && (col1 != col2)){
            if(board[row1][col2] == 0 && isDirectlyConnected(row1, col1, row1, col2, board)
            && isDirectlyConnected(row1, col2, row2, col2, board))
                return true;
            if(board[row2][col1] == 0 && isDirectlyConnected(row2, col2, row2, col1, board)
            && isDirectlyConnected(row2, col1, row1, col1, board))
                return true;
        }

        // three lines
        if(row1 != row2)
            for (int i = 0; i < board[0].length; i++) {
                if (board[row1][i] == 0 && board[row2][i] == 0 &&
                        isDirectlyConnected(row1, col1, row1, i, board) && isDirectlyConnected(row1, i, row2, i, board)
                        && isDirectlyConnected(row2, col2, row2, i, board)){
                    return true;
                }
            }
        if(col1 != col2)
            for (int j = 0; j < board.length; j++){
                if (board[j][col1] == 0 && board[j][col2] == 0 &&
                        isDirectlyConnected(row1, col1, j, col1, board) && isDirectlyConnected(j, col1, j, col2, board)
                        && isDirectlyConnected(row2, col2, j, col2, board)){
                    return true;
                }
            }
        return false;
    }

    // judge whether
    private static boolean isDirectlyConnected(int row1, int col1, int row2, int col2, int[][] board) {
        if (row1 == row2) {
            int minCol = Math.min(col1, col2);
            int maxCol = Math.max(col1, col2);
            for (int col = minCol + 1; col < maxCol; col++) {
                if (board[row1][col] != 0) {
                    return false;
                }
            }
            return true;
        } else if (col1 == col2) {
            int minRow = Math.min(row1, row2);
            int maxRow = Math.max(row1, row2);
            for (int row = minRow + 1; row < maxRow; row++) {
                if (board[row][col1] != 0) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public List<int[]> getPath(int row1, int col1, int row2, int col2) {
        List<int[]> path = new ArrayList<>();
        if (!judge(row1, col1, row2, col2)) {
            return path; // 如果不合法，返回空路径
        }

        path.add(new int[]{row1, col1}); // 添加起点

        // 直接连接的情况
        if (isDirectlyConnected(row1, col1, row2, col2, board)) {
            path.add(new int[]{row2, col2});
            return path;
        }

        // 一折连接的情况
        if (row1 != row2 && col1 != col2) {
            // 尝试通过中间某一点来形成一折路径
            if (board[row1][col2] == 0 && isDirectlyConnected(row1, col1, row1, col2, board)
                    && isDirectlyConnected(row1, col2, row2, col2, board)) {
                path.add(new int[]{row1, col2});
                path.add(new int[]{row2, col2});
                return path;
            }

            if (board[row2][col1] == 0 && isDirectlyConnected(row2, col2, row2, col1, board)
                    && isDirectlyConnected(row2, col1, row1, col1, board)) {
                path.add(new int[]{row2, col1});
                path.add(new int[]{row2, col2});
                return path;
            }
        }

        // 两折连接的情况
        // 通过扫描每一行和列找到可以通过两个转折点连接的路径
        for (int i = 0; i < board[0].length; i++) {
            if (board[row1][i] == 0 && board[row2][i] == 0
                    && isDirectlyConnected(row1, col1, row1, i, board)
                    && isDirectlyConnected(row1, i, row2, i, board)
                    && isDirectlyConnected(row2, col2, row2, i, board)) {
                path.add(new int[]{row1, i});
                path.add(new int[]{row2, i});
                path.add(new int[]{row2, col2});
                return path;
            }
        }

        for (int j = 0; j < board.length; j++) {
            if (board[j][col1] == 0 && board[j][col2] == 0
                    && isDirectlyConnected(row1, col1, j, col1, board)
                    && isDirectlyConnected(j, col1, j, col2, board)
                    && isDirectlyConnected(row2, col2, j, col2, board)) {
                path.add(new int[]{j, col1});
                path.add(new int[]{j, col2});
                path.add(new int[]{row2, col2});
                return path;
            }
        }

        return path; // 如果没有找到有效的路径，返回一个仅包含起点的路径
    }

}
