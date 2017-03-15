# scrabulous - Scrabble in Clojure

scrabulous is a [Clojure](http://clojure.org) project that implements the board game Scrabble.
You can use scrabulous to play a game of Scrabble with up to four players. You can also use it
to recover moves in a previously-played game.

The inspiration for scrabulous struck at a game night event at the company where I work. I played
a game of Scrabble with a coworker (he beat me by less than 10 points). At the end of the game, I
started looking at the score sheet we had kept, which consisted of the number of points scored by
each player on each turn. I looked at the score sheet and looked at the finished Scrabble board,
and I started wondering if it would be possible to use that information to work out which words
had been played by which player. Could I match up numeric scores to actual words played using
only the rules of the game, the final board, and a bit of cleverness? What emerged is scrabulous.

Not only did I get myself completely entangled in solving Scrabble puzzles, but I mentioned this
question of recovering games to some of my coworkers and managed to completely [nerd-snipe](https://xkcd.com/356/)
them in the process! You can see their implementations elsewhere on GitHub:
* https://github.com/benjamincrom/scrabble
* https://github.com/shapr/schrabble
* https://github.com/zabracks/scraffle

## Installation

Download from [Github](https://github.com/will2dye4/scrabulous). There is currently no way to "install"
scrabulous as a library or package; it is intended to be used from the Clojure REPL (see below).

You should have [Leiningen](https://leiningen.org) installed to use scrabulous.

## Usage

Launch a REPL via Leiningen to use scrabulous.

    $ lein repl

Once you're in the REPL, you can play a game, load a game from a file, recover moves from a finished
game, etc. See **Examples** below for details.

## Examples

### Playing a game

When you start a new REPL session, there will be an atom called `game` which represents a new game of
Scrabble for two players. The `-main` function in `scrabulous.core` simply prints the current state of
the game to stdout.

```
scrabulous.core=> (-main)
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  1 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  2 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  3 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  4 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  5 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  6 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  7 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  8 |   |   |   |   |   |   |   | * |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  9 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 10 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 11 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 12 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 13 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 14 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 15 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
      A   B   C   D   E   F   G   H   I   J   K   L   M   N   O

Player 1 ( 0 points )
[A A E E E N R]
Player 2 ( 0 points )
[A D I I O T Y]

86 tiles remaining
Player 1 to move
nil
```

(The real output will be colorized, showing where the multiplier squares are located!)

From here, there are several functions available in `scrabulous.core` for playing the game.
The most common one is `play`, which will attempt to play the specified word for the current
player and update the state of the `game` atom if successful.

To play a word, you must specify the coordinates on the board where the word will start, the
direction (across or down), and the full word (including any letters that are on the board
already that you are playing through). Keep in mind that the opening move must go through the
center square (H8, marked with an asterisk), and subsequent moves must be adjacent to one or
more tiles already on the board.

```
scrabulous.core=> (play H8 across near)
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  1 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  2 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  3 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  4 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  5 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  6 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  7 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  8 |   |   |   |   |   |   |   | N | E | A | R |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  9 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 10 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 11 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 12 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 13 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 14 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 15 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
      A   B   C   D   E   F   G   H   I   J   K   L   M   N   O

Player 1 ( 8 points )
[A E E G I O _]
Player 2 ( 0 points )
[A D I I O T Y]

82 tiles remaining
Player 2 to move
nil
scrabulous.core=> (play H5 down dainty)
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  1 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  2 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  3 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  4 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  5 |   |   |   |   |   |   |   | D |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  6 |   |   |   |   |   |   |   | A |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  7 |   |   |   |   |   |   |   | I |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  8 |   |   |   |   |   |   |   | N | E | A | R |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  9 |   |   |   |   |   |   |   | T |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 10 |   |   |   |   |   |   |   | Y |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 11 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 12 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 13 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 14 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 15 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
      A   B   C   D   E   F   G   H   I   J   K   L   M   N   O

Player 1 ( 8 points )
[A E E G I O _]
Player 2 ( 10 points )
[E I L O O O X]

77 tiles remaining
Player 1 to move
nil
```

As you can see, `play` prints the updated game state after making the requested move.
If you are playing one or more blank tiles as part of your move, use underscore in the
word to indicate where the blank tile(s) should go, and pass a vector of one-letter strings
to `play` indicating which letter(s) the blank tile(s) are standing in for.

```
scrabulous.core=> (play E5 across goade_ ["d"])
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  1 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  2 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  3 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  4 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  5 |   |   |   |   | G | O | A | D | E | d |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  6 |   |   |   |   |   |   |   | A |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  7 |   |   |   |   |   |   |   | I |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  8 |   |   |   |   |   |   |   | N | E | A | R |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  9 |   |   |   |   |   |   |   | T |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 10 |   |   |   |   |   |   |   | Y |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 11 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 12 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 13 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 14 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 15 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
      A   B   C   D   E   F   G   H   I   J   K   L   M   N   O

Player 1 ( 22 points )
[E E I M O S Y]
Player 2 ( 10 points )
[E I L O O O X]

72 tiles remaining
Player 2 to move
nil
```

Blank tiles are printed in lowercase to distinguish them from normal tiles.

Other functions available for game play are `pass`, which passes the current player's turn
without making a move, and `exchange`, which exchanges one or more of the current player's tiles
for new tiles (drawn from the tile bag). Exchanging tiles counts as the current player's turn, i.e.,
you cannot both exchange tiles and play a word on the same turn.

```
scrabulous.core=> (pass)
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  1 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  2 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  3 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  4 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  5 |   |   |   |   | G | O | A | D | E | d |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  6 |   |   |   |   |   |   |   | A |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  7 |   |   |   |   |   |   |   | I |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  8 |   |   |   |   |   |   |   | N | E | A | R |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  9 |   |   |   |   |   |   |   | T |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 10 |   |   |   |   |   |   |   | Y |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 11 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 12 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 13 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 14 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 15 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
      A   B   C   D   E   F   G   H   I   J   K   L   M   N   O

Player 1 ( 22 points )
[E E I M O S Y]
Player 2 ( 10 points )
[E I L O O O X]

72 tiles remaining
Player 1 to move
nil
scrabulous.core=> (exchange ["e" "y"])
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  1 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  2 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  3 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  4 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  5 |   |   |   |   | G | O | A | D | E | d |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  6 |   |   |   |   |   |   |   | A |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  7 |   |   |   |   |   |   |   | I |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  8 |   |   |   |   |   |   |   | N | E | A | R |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  9 |   |   |   |   |   |   |   | T |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 10 |   |   |   |   |   |   |   | Y |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 11 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 12 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 13 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 14 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 15 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
      A   B   C   D   E   F   G   H   I   J   K   L   M   N   O

Player 1 ( 22 points )
[E I I M O R S]
Player 2 ( 10 points )
[E I L O O O X]

72 tiles remaining
Player 2 to move
nil
```

### Creating a new game

You can use the `create-game` function to create a new game. You can customize almost all
parameters of the game, but the only required argument to `create-game` is the number of players.

```
scrabulous.core=> (def four-player-game (create-game 4))
#'scrabulous.core/four-player-game
scrabulous.core=> (count (:players four-player-game))
4
```

To manipulate a game created with `create-game` using the scrabulous API, it is necessary to
wrap the game in an atom. Then you can use the functions defined in `scrabulous.game` to play,
pass, and exchange as described in the previous section.

```
scrabulous.core=> (def another-game (atom (create-game 3)))
#'scrabulous.core/another-game
scrabulous.core=> (print-state @another-game)
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  1 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  2 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  3 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  4 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  5 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  6 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  7 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  8 |   |   |   |   |   |   |   | * |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  9 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 10 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 11 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 12 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 13 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 14 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 15 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
      A   B   C   D   E   F   G   H   I   J   K   L   M   N   O

Player 1 ( 0 points )
[B C D M U Y _]
Player 2 ( 0 points )
[E F I K N P R]
Player 3 ( 0 points )
[A E F L R S Y]

79 tiles remaining
Player 1 to move
nil
scrabulous.core=> (play! another-game [:H 8] :across "dumb")
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  1 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  2 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  3 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  4 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  5 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  6 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  7 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  8 |   |   |   |   |   |   |   | D | U | M | B |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
  9 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 10 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 11 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 12 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 13 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 14 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 15 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
      A   B   C   D   E   F   G   H   I   J   K   L   M   N   O

Player 1 ( 18 points )
[A C E S Y Z _]
Player 2 ( 0 points )
[E F I K N P R]
Player 3 ( 0 points )
[A E F L R S Y]

75 tiles remaining
Player 2 to move
nil
```

### Loading and saving games

The `scrabulous.serializer` namespace provides functions for loading and saving games
in JSON format. The save format does not store the entire game state, but rather only
the bits of information needed to recover the moves later (see below). The `load-game`
function reads a JSON file and tries to convert it into a scrabulous game that can be
used for move recovery. There are sample input files (different states of one game)
located in the `resources/sample_inputs` directory.

```
scrabulous.core=> (def finished-game (load-game "resources/sample_inputs/sample_input1.json"))
#'scrabulous.core/finished-game
scrabulous.core=> (:players finished-game)
{1 {:tile-rack [], :score 30, :moves [{:total 30, :words [["?" 30]]}]}, 2 {:tile-rack [], :score 0, :moves []}}
```

The `save-game` function accepts a game and a filepath and saves the game to the specified path.

```
scrabulous.core=> (require '[scrabulous.serializer :refer [save-game]])
nil
scrabulous.core=> (save-game another-game "/tmp/another_game.json")
#object[java.io.BufferedWriter 0x6ac894a3 "java.io.BufferedWriter@6ac894a3"]
```

(`save-game` returns a `BufferedWriter`, but the writer is already closed. The returned writer
should be discarded.)

### Recovering moves

The `scrabulous.recovery` namespace provides functions for recovering the moves played in a
finished game. The idea, as explained in the introduction at the top of this file, is to use only
the numeric score for each player on each turn combined with the finished board to deduce which
words were played by which player. The `recover-moves` function takes a finished game and returns
a lazy sequence of the unique games that could have been played to result in the provided final
game state. Often there will only be one unique game, but certain ambiguous situations may result
in multiple possible games being returned.

```
scrabulous.core=> (require '[clojure.pprint :as pprint])
nil
scrabulous.core=> (def input-game (load-game "resources/sample_inputs/sample_input30.json"))
#'scrabulous.core/input-game
scrabulous.core=> (def recovered-games (recover-moves input-game))
#'scrabulous.core/recovered-games
scrabulous.core=> (count recovered-games)
1
scrabulous.core=> (pprint (:players input-game))
{1
 {:tile-rack [],
  :score 162,
  :moves
  [{:total 30, :words [["?" 30]]}
   {:total 13, :words [["?" 13]]}
   {:total 8, :words [["?" 8]]}
   {:total 18, :words [["?" 18]]}
   {:total 7, :words [["?" 7]]}
   {:total 7, :words [["?" 7]]}
   {:total 8, :words [["?" 8]]}
   {:total 6, :words [["?" 6]]}
   {:total 10, :words [["?" 10]]}
   {:total 7, :words [["?" 7]]}
   {:total 10, :words [["?" 10]]}
   {:total 8, :words [["?" 8]]}
   {:total 3, :words [["?" 3]]}
   {:total 3, :words [["?" 3]]}
   {:total 24, :words [["?" 24]]}]},
 2
 {:tile-rack [],
  :score 283,
  :moves
  [{:total 22, :words [["?" 22]]}
   {:total 9, :words [["?" 9]]}
   {:total 27, :words [["?" 27]]}
   {:total 20, :words [["?" 20]]}
   {:total 16, :words [["?" 16]]}
   {:total 21, :words [["?" 21]]}
   {:total 20, :words [["?" 20]]}
   {:total 11, :words [["?" 11]]}
   {:total 18, :words [["?" 18]]}
   {:total 24, :words [["?" 24]]}
   {:total 21, :words [["?" 21]]}
   {:total 10, :words [["?" 10]]}
   {:total 36, :words [["?" 36]]}
   {:total 11, :words [["?" 11]]}
   {:total 17, :words [["?" 17]]}]}}
nil
scrabulous.core=> (pprint (:players (first recovered-games)))
{1
 {:tile-rack [],
  :score 162,
  :moves
  [{:total 30, :words [["_OWDY" 30]]}
   {:total 13, :words [["RAZE" 13]]}
   {:total 8, :words [["YIP" 8]]}
   {:total 18, :words [["MOVE" 18]]}
   {:total 7, :words [["GUY" 7]]}
   {:total 7, :words [["DOTE" 7]]}
   {:total 8, :words [["GUILE" 8]]}
   {:total 6, :words [["BEER" 6]]}
   {:total 10, :words [["RIP" 10]]}
   {:total 7, :words [["HINT" 7]]}
   {:total 10, :words [["DUNE" 10]]}
   {:total 8, :words [["PITA" 8]]}
   {:total 3, :words [["OR_" 3]]}
   {:total 3, :words [["LIT" 3]]}
   {:total 24, :words [["BANANA" 24]]}]},
 2
 {:tile-rack [],
  :score 283,
  :moves
  [{:total 22, :words [["FAMED" 22]]}
   {:total 9, :words [["CRIE_" 9]]}
   {:total 27, :words [["TIGHT" 20] ["IT" 3] ["PI" 4]]}
   {:total 20, :words [["GRAYER" 20]]}
   {:total 16, :words [["DASH" 16]]}
   {:total 21, :words [["OVEN" 21]]}
   {:total 20, :words [["JAR" 20]]}
   {:total 11, :words [["JIG" 11]]}
   {:total 18, :words [["LOOMS" 18]]}
   {:total 24, :words [["DOWEL" 18] ["RIPE" 6]]}
   {:total 21, :words [["REASON" 21]]}
   {:total 10, :words [["TAX" 10]]}
   {:total 36, :words [["EXIT" 36]]}
   {:total 11, :words [["ID" 9] ["IS" 2]]}
   {:total 17, :words [["SE_K" 17]]}]}}
nil
```

As you can see, the scores match exactly between the input game and the recovered game,
but whereas we didn't know the words played in the input game (because it was loaded from
a JSON file), the recovered game includes the words that were played on each turn to yield
those scores. You'll have to take my word for it that those are, in fact, the correct words...or
you can try it for yourself with a game you've played!

## License

Copyright © 2017 William Dye

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
