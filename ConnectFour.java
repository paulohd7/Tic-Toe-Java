package four;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static four.ConnectFour.COLS_LABEL_BOARD;

public class ConnectFour extends JFrame {

    boolean playerOne = true;
    boolean playerTwo = false;
    Board gamePanel;
    public static Character[] COLS_LABEL_BOARD = {'A', 'B', 'C', 'D', 'E', 'F', 'G'};
    int MAX_BOARD_ROWS = 6;
    int MAX_BOARD_COLUMNS = COLS_LABEL_BOARD.length;

    public ConnectFour() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 400);
        setResizable(false);
        setTitle("Connect Four");
        setLayout(new BorderLayout());
        add(prepareBoard(), BorderLayout.CENTER);
        add(resetButton(), BorderLayout.PAGE_END);
        setVisible(true);
    }

    private JButton resetButton() {
        JButton resetButton = new JButton();
        resetButton.setSize(30,10);
        resetButton.setName("ButtonReset");
        resetButton.setText("Reset");

        resetButton.addActionListener(e -> {
            gamePanel.getBoard().forEach(cellButton -> {
                int rowPos = cellButton.getRowPosition();
                int colPos = cellButton.getColPosition();
                cellButton.setLabel(" ");
                cellButton.setBackground(Color.DARK_GRAY);
                cellButton.setIsNext(rowPos == 0 ? true : false);
                gamePanel.resetPosition(rowPos, colPos);
            });
            resetTurn();
            gamePanel.unlockPanel();
        });

        return resetButton;
    }

    public void playOneTurn(CellButton buttonFree, PlayerSymbols activePlayer, int rowFree, int colFree) {
        playerAction(buttonFree, activePlayer, rowFree, colFree);
        gamePanel.checkStreaks(rowFree, colFree, activePlayer);
        setNextPlayer();
    }

    private void resetTurn() {
        playerOne = true;
        playerTwo = false;
    }

    private void setNextPlayer() {
        playerOne = !playerOne;
        playerTwo = !playerTwo;
    }

    private void playerAction(CellButton cellFree, PlayerSymbols activePlayer, int rowFree, int colFree) {
        cellFree.setPlayerSymbol(activePlayer.toString());
        gamePanel.setPosition(rowFree, colFree, activePlayer);
        gamePanel.setNextFreePosition(cellFree);
    }

    private JPanel prepareBoard() {

        JPanel panelCells = new JPanel(new GridLayout(MAX_BOARD_ROWS,MAX_BOARD_COLUMNS, 2, 2));
        panelCells.setBackground(Color.GRAY);

        BiFunction<Integer, Integer, JButton> makeCell = (rowNumber, colNumber) -> {
            JButton buttonCell = new CellButton(rowNumber, colNumber);

                buttonCell.addActionListener(e -> {
                    if (panelCells.isEnabled()) {
                        Optional<CellButton> nextFree = gamePanel.freePosition(colNumber);
                        int row = nextFree.get().getRowPosition();
                        int col = nextFree.get().getColPosition();
                        PlayerSymbols activePlayer = playerOne == true ? PlayerSymbols.PLAYER_ONE : PlayerSymbols.PLAYER_TWO;

                        nextFree.ifPresent(cellButton ->  playOneTurn(nextFree.get(), activePlayer, row, col));
                    }
                });
            return buttonCell;
        };

        IntStream.iterate(MAX_BOARD_ROWS, i -> i - 1)
                .limit(MAX_BOARD_ROWS)
                .forEach(rowNum -> IntStream.range(0, MAX_BOARD_COLUMNS)
                        .forEach(colNum -> panelCells.add(makeCell.apply(rowNum, colNum))));

        gamePanel = new Board(panelCells, MAX_BOARD_ROWS, MAX_BOARD_COLUMNS);

        return panelCells;
    }
}

class Board {

    private int maxRows;
    private int maxColumns;
    private final int MAX_SEQUENCE = 4;
    private JPanel boardPanel;
    private int[][] boardArray;

    Board (JPanel boardPanel, int maxRows, int maxColumns) {
        this.boardPanel = boardPanel;
        this.maxRows = maxRows;
        this.maxColumns = maxColumns;
        this.boardArray = new int[maxRows][maxColumns];
    }

    private void lockPanel() {
        this.boardPanel.setEnabled(false);
    }

    public void unlockPanel() {
        this.boardPanel.setEnabled(true);
    }

    public void checkStreaks(int row, int col, PlayerSymbols activePlayer) {
        checkHorizontalSequence(row, col, activePlayer);
        checkVerticalSequence(row, col, activePlayer);
        checkDiagonalSequenceLeft(row, col, activePlayer);
        checkDiagonalSequenceRight(row, col, activePlayer);
    }

    public Stream<CellButton> getBoard() {
        return Stream.of(this.boardPanel.getComponents())
                .filter(CellButton.class::isInstance)
                .map(CellButton.class::cast);
    }

    public void setPosition(int rowNum, int colNum, PlayerSymbols activePlayer) {
        boardArray[rowNum][colNum] = activePlayer.getPlayerNumber();
        printBoardArray();
    }

    public void resetPosition(int rowNum, int colNum) {
        boardArray[rowNum][colNum] = 0;
        printBoardArray();
    }

    // Debug method
    private void printBoardArray() {
        for (int x = 0; x < boardArray.length; x++) {
            for (int y = 0; y < boardArray[x].length; y++) {
                System.out.print(boardArray[x][y] + " ");
            }
            System.out.println();
        }
    }

    public Optional<CellButton> freePosition (int colNum) {
        return getBoard()
                .filter(cellButton -> cellButton.getColPosition() == colNum)
                .filter(cellButton -> cellButton.isNext() == true)
                .findFirst();
    }

    void checkDiagonalSequenceLeft(int rowNum, int colNum, PlayerSymbols activePlayer) {
        int numPlayer = activePlayer.getPlayerNumber();
        int rowStart = rowNum;
        int colStart = colNum;
        int boardSize = boardArray.length;
        int boardColSize = boardArray[0].length;
        Map<Integer, Integer> cellStreakMap = new HashMap<>();
        cellStreakMap.put(rowNum, colNum);

        while (rowStart > 0 && colStart > 0 && cellStreakMap.size() < MAX_SEQUENCE) {
            rowStart = rowStart - 1 >= 0 ? rowStart - 1 : 0;
            colStart = colStart - 1 >= 0 ? colStart - 1 : 0;
            if (boardArray[rowStart][colStart] == numPlayer) {
                cellStreakMap.put(rowStart, colStart);
            } else {
                break;
            }
        }

        rowStart = rowNum;
        colStart = colNum;

        while (rowStart < boardSize - 1 && colStart < boardColSize - 1 && cellStreakMap.size() < MAX_SEQUENCE) {
            rowStart = rowStart + 1 > boardSize ? rowStart + 1 : boardSize - 1;
            colStart = colStart + 1 < boardColSize ? colStart + 1 : boardColSize - 1;
            if (boardArray[rowStart][colStart] == numPlayer) {
                cellStreakMap.put(rowStart, colStart);
            } else {
                break;
            }
        }

        fillDiagonalCells(cellStreakMap, activePlayer.getPlayerColor());
        //System.out.println(streak);
    }

    void checkDiagonalSequenceRight(int rowNum, int colNum, PlayerSymbols activePlayer) {
        int numPlayer = activePlayer.getPlayerNumber();
        int rowToCheck = rowNum;
        int colToCheck = colNum;
        int boardSize = boardArray.length;
        int boardColSize = boardArray[0].length;
        Map<Integer, Integer> cellStreakMap = new HashMap<>();
        cellStreakMap.put(rowNum, colNum);

        // Check Second Diagonal
        while (rowToCheck > 0 && colToCheck < boardColSize && cellStreakMap.size() < MAX_SEQUENCE) {;
            System.out.println("check d");
            rowToCheck = rowToCheck - 1 >= 0 ? rowToCheck - 1 : 0;
            colToCheck = colToCheck + 1 < boardColSize ? colToCheck + 1 : boardColSize - 1;
            System.out.println("d" + rowToCheck);
            System.out.println("d" + colToCheck);
            if (boardArray[rowToCheck][colToCheck] == numPlayer) {
                cellStreakMap.put(rowToCheck, colToCheck);
            } else {
                break;
            }
        }

        rowToCheck = rowNum;
        colToCheck = colNum;

        while (rowToCheck < boardSize - 1 && colToCheck > 0 && cellStreakMap.size() < MAX_SEQUENCE) {

            rowToCheck = rowToCheck + 1 > boardSize ? rowToCheck + 1 : boardSize - 1;
            colToCheck = colToCheck - 1 > 0 ? colToCheck - 1 : 0;
            if (boardArray[rowToCheck][colToCheck] == numPlayer) {
                cellStreakMap.put(rowToCheck, colToCheck);
            } else {
                break;
            }
        }

        fillDiagonalCells(cellStreakMap, activePlayer.getPlayerColor());
    }

    private void fillDiagonalCells(Map<Integer, Integer> cellStreakMap, Color playerColor) {
        if (cellStreakMap.size() == MAX_SEQUENCE) {
            cellStreakMap.entrySet().stream().forEach(positionMap -> getBoard()
                    .filter(cellButton -> cellButton.getRowPosition() == positionMap.getKey() && cellButton.getColPosition() == positionMap.getValue())
                    .forEach(cellbutton -> cellbutton.setBackground(playerColor)));
            lockPanel();
        }
    }

    public void setNextFreePosition(CellButton lastFreePosition) {
        lastFreePosition.setIsNext(false);
        this.getBoard()
                .filter(cellButton -> (cellButton.getColPosition() == lastFreePosition.getColPosition() && cellButton.getRowPosition() == lastFreePosition.getRowPosition() + 1))
                .findFirst()
                .ifPresent(cellButton -> cellButton.setIsNext(true));
    }

    public void checkHorizontalSequence(int rowPos, int colPos, PlayerSymbols activePlayer) {
        int numPlayer = activePlayer.getPlayerNumber();
        int rangeStart = (colPos - 3) >= 0 ? (colPos - 3) : 0;
        int rangeEnd = (colPos + 3) < (maxColumns - 1) ? (colPos + 3) : (maxColumns - 1);
        List<Integer> colsStreak = new ArrayList<>();

        for (int x = rangeStart; colsStreak.size() < MAX_SEQUENCE && x <= rangeEnd; x++) {
            if (boardArray[rowPos][x] == numPlayer) {
                colsStreak.add(x);
            } else {
                colsStreak.clear();
            }
        }

        if (colsStreak.size() == MAX_SEQUENCE) {
            getBoard()
                    .filter(cellButton -> cellButton.getRowPosition() == rowPos)
                    .filter(cellButton -> cellButton.getColPosition() >= colsStreak.get(0) && cellButton.getColPosition() <= colsStreak.get(3))
                    .forEach(cellButton -> cellButton.setBackground(activePlayer.getPlayerColor()));
            lockPanel();
        }
    }

    public void checkVerticalSequence(int rowPos, int colPos, PlayerSymbols activePlayer) {
        int numPlayer = activePlayer.getPlayerNumber();
        int rangeStart = (rowPos - 3) >= 0 ? (rowPos - 3) : 0;
        int rangeEnd = (rowPos + 3) < (maxRows - 1) ? (rowPos + 3) : (maxRows - 1);
        List<Integer> rowsStreak = new ArrayList<>();

        for (int x = rangeStart; rowsStreak.size() < MAX_SEQUENCE && x <= rangeEnd; x++) {
            if (boardArray[x][colPos] == numPlayer) {
                rowsStreak.add(x);
            } else {
                rowsStreak.clear();
            }
        }

        if (rowsStreak.size() == MAX_SEQUENCE) {
            getBoard()
                    .filter(cellButton -> cellButton.getColPosition() == colPos)
                    .filter(cellButton -> cellButton.getRowPosition() >= rowsStreak.get(0) && cellButton.getRowPosition() <= rowsStreak.get(3))
                    .forEach(cellButton -> cellButton.setBackground(activePlayer.getPlayerColor()));
            lockPanel();
        }
    }

}

class CellButton extends JButton {

    private int FIRST_ROW_BOARD = 1;
    private int rowPosition;
    private int colPosition;
    private boolean isNext;

    private Point boardPosition;

    public CellButton(int rowPosition, int colPosition) {
        this.rowPosition = rowPosition - 1;
        this.colPosition = colPosition;
        this.boardPosition = new Point(colPosition, rowPosition - 1);
        this.isNext = rowPosition == FIRST_ROW_BOARD  ? true : false;
        this.setBounds(0,0,250,250);
        this.setFocusPainted(false);
        this.setBorderPainted(false);
        this.setForeground(Color.white);
        this.setBackground(Color.darkGray);
        this.setName("Button" + COLS_LABEL_BOARD[colPosition] + rowPosition);
        this.setLabel(" ");
    }

    public int getColPosition() {
        return this.colPosition;
    }

    public int getRowPosition() {
        return this.rowPosition;
    }

    public boolean isNext() {
        return this.isNext;
    }

    public void setIsNext(boolean isNext) {
        this.isNext = isNext;
    }

    public void setPlayerSymbol(String playerSymbol) {
        this.setLabel(playerSymbol);
    }

}

enum PlayerSymbols {
    PLAYER_ONE("X", 1, Color.GREEN), PLAYER_TWO("O", 2, Color.BLUE);

    PlayerSymbols(String x, int i, Color playerColor) {
        this.symbolIcon = x;
        this.symbolNumber = i;
        this.playerColor = playerColor;
    }

    private String symbolIcon;
    private int symbolNumber;
    private Color playerColor;

    @Override
    public String toString() {
        return this.symbolIcon;
    }

    public int getPlayerNumber() {
        return this.symbolNumber;
    }

    public Color getPlayerColor() { return this.playerColor; }
}