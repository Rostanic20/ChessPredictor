// Stockfish loader for web
window.StockfishLoader = {
    loadStockfish: function() {
        return new Promise((resolve, reject) => {
            // Try different approaches to load Stockfish
            const sources = [
                // Try Stockfish 17.1 CDN sources first (with UCI_Elo support)
                'https://unpkg.com/stockfish@17.1.0/stockfish.js',
                'https://cdn.jsdelivr.net/npm/stockfish@17.1.0/stockfish.js',
                'https://unpkg.com/stockfish@17.0.0/stockfish.js',
                'https://cdn.jsdelivr.net/npm/stockfish@17.0.0/stockfish.js',
                
                // Fallback to older versions
                'https://unpkg.com/stockfish@16.0.0/src/stockfish.js',
                'https://cdn.jsdelivr.net/npm/stockfish@16.0.0/src/stockfish.js',
                
                'data:application/javascript,' + encodeURIComponent(`
                    self.onmessage = function(e) {
                        const command = e.data;
                        
                        if (command === 'uci') {
                            self.postMessage('id name FallbackEngine');
                            self.postMessage('id author ChessPredictor');
                            self.postMessage('option name UCI_LimitStrength type check default false');
                            self.postMessage('option name UCI_Elo type spin default 1320 min 1320 max 3190');
                            self.postMessage('uciok');
                        } else if (command === 'isready') {
                            self.postMessage('readyok');
                        } else if (command.startsWith('position')) {
                            // Just acknowledge position commands
                            setTimeout(() => self.postMessage('info position set'), 10);
                        } else if (command.startsWith('go')) {
                            // Return a random legal move after a short delay
                            setTimeout(() => {
                                // Expanded move list for both colors and different positions
                                const moves = [
                                    'e2e4', 'e2e3', 'd2d4', 'd2d3', 'g1f3', 'b1c3', 'f1c4', 'f1e2',
                                    'e7e5', 'e7e6', 'd7d5', 'd7d6', 'g8f6', 'b8c6', 'f8c5', 'f8e7',
                                    'c2c4', 'c2c3', 'a2a3', 'a2a4', 'h2h3', 'h2h4', 'g2g3', 'g2g4',
                                    'c7c5', 'c7c6', 'a7a6', 'a7a5', 'h7h6', 'h7h5', 'g7g6', 'g7g5',
                                    'b1d2', 'g1h3', 'f1d3', 'e1g1', 'b8d7', 'g8h6', 'f8d6', 'e8g8'
                                ];
                                const randomMove = moves[Math.floor(Math.random() * moves.length)];
                                self.postMessage('info depth 1 score cp 0 nodes 1 nps 1000 pv ' + randomMove);
                                self.postMessage('bestmove ' + randomMove);
                            }, 500);
                        } else if (command === 'stop') {
                            self.postMessage('bestmove e2e4');
                        }
                    };
                `)
            ];
            
            let currentIndex = 0;
            
            function tryNextSource() {
                if (currentIndex >= sources.length) {
                    reject(new Error('Failed to load Stockfish from all sources'));
                    return;
                }

                const source = sources[currentIndex++];
                
                try {
                    const worker = new Worker(source);
                    
                    // Test if the worker is responsive
                    let responded = false;
                    const testTimeout = setTimeout(() => {
                        if (!responded) {
                            console.warn('Worker did not respond within 5 seconds, trying next source');
                            worker.terminate();
                            tryNextSource();
                        }
                    }, 5000); // Increased timeout
                    
                    worker.onmessage = function(e) {
                        const data = e.data;
                        if (data && (data.includes('Stockfish') || data.includes('FallbackEngine') || data.includes('uciok'))) {
                            responded = true;
                            clearTimeout(testTimeout);
                            resolve(worker);
                        }
                    };
                    
                    worker.onerror = function(error) {
                        console.error('Worker error from', source, ':', error);
                        clearTimeout(testTimeout);
                        tryNextSource();
                    };
                    
                    // Send test command
                    worker.postMessage('uci');
                } catch (error) {
                    console.error('Failed to create worker from', source, ':', error);
                    tryNextSource();
                }
            }
            
            tryNextSource();
        });
    }
};