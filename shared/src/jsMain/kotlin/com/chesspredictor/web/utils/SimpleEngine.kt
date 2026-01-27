package com.chesspredictor.web.utils

import org.w3c.dom.Worker
import kotlinx.browser.window

/**
 * Creates a simple, self-contained chess engine that works without external dependencies
 */
fun createSimpleEngine(): Worker {
    val engineCode = """
        // Simple UCI chess engine implementation
        let position = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';
        let skillLevel = 1;
        
        const openingBook = {
            'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1': ['e2e4', 'd2d4', 'g1f3', 'c2c4'],
            'rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1': ['e7e5', 'c7c5', 'e7e6', 'd7d6'],
            'rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1': ['d7d5', 'g8f6', 'c7c5', 'e7e6'],
        };
        
        function getRandomMove(moves) {
            return moves[Math.floor(Math.random() * moves.length)];
        }
        
        function getBestMove(pos) {
            // Check opening book first
            if (openingBook[pos]) {
                return getRandomMove(openingBook[pos]);
            }
            
            // Basic move generation for common positions
            const commonMoves = [
                'e2e4', 'e2e3', 'd2d4', 'd2d3', 'g1f3', 'b1c3', 'f1c4', 'f1e2',
                'e7e5', 'e7e6', 'd7d5', 'd7d6', 'g8f6', 'b8c6', 'f8c5', 'f8e7'
            ];
            
            return getRandomMove(commonMoves);
        }
        
        self.onmessage = function(e) {
            const command = e.data.trim();
            
            if (command === 'uci') {
                self.postMessage('id name SimpleEngine 1.0');
                self.postMessage('id author ChessPredictor');
                self.postMessage('option name Skill Level type spin default 1 min 0 max 20');
                self.postMessage('option name Hash type spin default 16 min 1 max 128');
                self.postMessage('option name Threads type spin default 1 min 1 max 1');
                self.postMessage('uciok');
                
            } else if (command === 'isready') {
                self.postMessage('readyok');
                
            } else if (command.startsWith('setoption name Skill Level value ')) {
                skillLevel = parseInt(command.split(' ').pop()) || 1;
                
            } else if (command.startsWith('setoption')) {
                // Acknowledge other options
                
            } else if (command.startsWith('position fen ')) {
                position = command.substring(13);
                
            } else if (command === 'position startpos' || command.startsWith('position startpos moves')) {
                position = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';
                
            } else if (command.startsWith('go')) {
                // Simulate thinking time based on skill level
                const thinkTime = Math.max(200, Math.min(2000, skillLevel * 100 + Math.random() * 500));
                
                setTimeout(() => {
                    try {
                        const move = getBestMove(position);
                        const score = Math.floor((Math.random() - 0.5) * 100);

                        self.postMessage('info depth 1 score cp ' + score + ' nodes 100 nps 1000 time ' + Math.floor(thinkTime) + ' pv ' + move);
                        self.postMessage('bestmove ' + move);
                    } catch (error) {
                        console.error('SimpleEngine: Error generating move:', error);
                        self.postMessage('bestmove e2e4'); // Fallback move
                    }
                }, thinkTime);
                
            } else if (command === 'stop') {
                const move = getBestMove(position);
                self.postMessage('bestmove ' + move);
                
            } else if (command === 'quit') {
                self.close();
            }
        };
        
        // Signal that engine is ready
        setTimeout(() => {
            self.postMessage('SimpleEngine initialized');
        }, 100);
    """.trimIndent()
    
    val blob = js("new Blob([engineCode], {type: 'application/javascript'})")
    val url = js("URL.createObjectURL(blob)")
    
    return Worker(url.toString())
}