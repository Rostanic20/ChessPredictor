package com.chesspredictor.web.utils

import kotlinx.browser.window
import org.w3c.dom.Worker

fun createFallbackEngine(): Worker {
    val workerScript = """
        self.onmessage = function(e) {
            const command = e.data;
            
            if (command === 'uci') {
                self.postMessage('id name FallbackEngine 1.0');
                self.postMessage('id author ChessPredictor');
                self.postMessage('option name Skill Level type spin default 1 min 0 max 20');
                self.postMessage('uciok');
            } else if (command === 'isready') {
                self.postMessage('readyok');
            } else if (command.startsWith('setoption')) {
                // Acknowledge option setting
                self.postMessage('info string option set');
            } else if (command.startsWith('position')) {
                // Just acknowledge position commands
                self.postMessage('info string position set');
            } else if (command.startsWith('go')) {
                // Return a simple move after a delay
                setTimeout(() => {
                    // Basic opening moves for demonstration
                    const openingMoves = [
                        'e2e4', 'd2d4', 'g1f3', 'b1c3', 'f1c4', 
                        'e2e3', 'd2d3', 'g1h3', 'b1a3', 'f1e2'
                    ];
                    const randomMove = openingMoves[Math.floor(Math.random() * openingMoves.length)];
                    
                    self.postMessage('info depth 1 score cp 25 nodes 100 nps 1000 time 100 pv ' + randomMove);
                    self.postMessage('bestmove ' + randomMove);
                }, Math.random() * 800 + 200); // Random delay between 200-1000ms
            } else if (command === 'stop') {
                self.postMessage('bestmove e2e4');
            } else if (command === 'quit') {
                self.close();
            }
        };
        
        self.postMessage('FallbackEngine ready');
    """.trimIndent()
    
    val blob = js("new Blob([workerScript], {type: 'application/javascript'})")
    val workerUrl = js("URL.createObjectURL(blob)")
    
    return Worker(workerUrl.toString())
}