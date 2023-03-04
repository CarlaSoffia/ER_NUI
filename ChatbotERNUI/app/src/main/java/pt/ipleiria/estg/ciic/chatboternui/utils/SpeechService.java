/*     */ package pt.ipleiria.estg.ciic.chatboternui.utils;
/*     */
/*     */ import android.annotation.SuppressLint;
import android.media.AudioRecord;
/*     */ import android.os.Handler;
/*     */ import android.os.Looper;
/*     */ import java.io.IOException;
/*     */ import org.vosk.Recognizer;
/*     */ import org.vosk.android.RecognitionListener;
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */ public class SpeechService
/*     */ {
/*     */   private final Recognizer recognizer;
/*     */   private final int sampleRate;
/*     */   private static final float BUFFER_SIZE_SECONDS = 0.2F;
/*     */   private final int bufferSize;
/*     */   private final AudioRecord recorder;
/*     */   private RecognizerThread recognizerThread;
/*  43 */   private final Handler mainHandler = new Handler(Looper.getMainLooper());
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */   @SuppressLint("MissingPermission")
public SpeechService(Recognizer recognizer, float sampleRate) throws IOException {
/*  52 */     this.recognizer = recognizer;
              this.recognizer.setWords(true);
/*  53 */     this.sampleRate = (int)sampleRate;
/*     */
/*  55 */     this.bufferSize = Math.round(this.sampleRate * 0.2F);
/*  56 */     this.recorder = new AudioRecord(6, this.sampleRate, 16, 2, this.bufferSize * 2);
/*     */ 
/*     */ 
/*     */ 
/*     */     
/*  61 */     if (this.recorder.getState() == 0) {
/*  62 */       this.recorder.release();
/*  63 */       throw new IOException("Failed to initialize recorder. Microphone might be already in use.");
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean startListening(RecognitionListener listener) {
/*  75 */     if (null != this.recognizerThread) {
/*  76 */       return false;
/*     */     }
/*  78 */     this.recognizerThread = new RecognizerThread(listener);
/*  79 */     this.recognizerThread.start();
/*  80 */     return true;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean startListening(RecognitionListener listener, int timeout) {
/*  92 */     if (null != this.recognizerThread) {
/*  93 */       return false;
/*     */     }
/*  95 */     this.recognizerThread = new RecognizerThread(listener, timeout);
/*  96 */     this.recognizerThread.start();
/*  97 */     return true;
/*     */   }
/*     */   
/*     */   private boolean stopRecognizerThread() {
/* 101 */     if (null == this.recognizerThread) {
/* 102 */       return false;
/*     */     }
/*     */     try {
/* 105 */       this.recognizerThread.interrupt();
/* 106 */       this.recognizerThread.join();
/* 107 */     } catch (InterruptedException e) {
/*     */       
/* 109 */       Thread.currentThread().interrupt();
/*     */     } 
/*     */     
/* 112 */     this.recognizerThread = null;
/* 113 */     return true;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean stop() {
/* 123 */     return stopRecognizerThread();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean cancel() {
/* 133 */     if (this.recognizerThread != null) {
/* 134 */       this.recognizerThread.setPause(true);
/*     */     }
/* 136 */     return stopRecognizerThread();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void shutdown() {
/* 143 */     this.recorder.release();
/*     */   }
/*     */   
/*     */   public void setPause(boolean paused) {
/* 147 */     if (this.recognizerThread != null) {
/* 148 */       this.recognizerThread.setPause(paused);
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void reset() {
/* 156 */     if (this.recognizerThread != null) {
/* 157 */       this.recognizerThread.reset();
/*     */     }
/*     */   }
/*     */   
/*     */   private final class RecognizerThread
/*     */     extends Thread
/*     */   {
/*     */     private int remainingSamples;
/*     */     private final int timeoutSamples;
/*     */     private static final int NO_TIMEOUT = -1;
/*     */     private volatile boolean paused = false;
/*     */     private volatile boolean reset = false;
/*     */     RecognitionListener listener;
/*     */     
/*     */     public RecognizerThread(RecognitionListener listener, int timeout) {
/* 172 */       this.listener = listener;
/* 173 */       if (timeout != -1) {
/* 174 */         this.timeoutSamples = timeout * SpeechService.this.sampleRate / 1000;
/*     */       } else {
/* 176 */         this.timeoutSamples = -1;
/* 177 */       }  this.remainingSamples = this.timeoutSamples;
/*     */     }
/*     */     
/*     */     public RecognizerThread(RecognitionListener listener) {
/* 181 */       this(listener, -1);
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/*     */     public void setPause(boolean paused) {
/* 191 */       this.paused = paused;
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */     
/*     */     public void reset() {
/* 198 */       this.reset = true;
/*     */     }
/*     */ 
/*     */ 
/*     */     
/*     */     public void run() {
/* 204 */       SpeechService.this.recorder.startRecording();
/* 205 */       if (SpeechService.this.recorder.getRecordingState() == 1) {
/* 206 */         SpeechService.this.recorder.stop();
/* 207 */         IOException ioe = new IOException("Failed to start recording. Microphone might be already in use.");
/*     */         
/* 209 */         SpeechService.this.mainHandler.post(() -> this.listener.onError(ioe));
/*     */       } 
/*     */       
/* 212 */       short[] buffer = new short[SpeechService.this.bufferSize];
/*     */       
/* 214 */       while (!interrupted() && (this.timeoutSamples == -1 || this.remainingSamples > 0)) {
/*     */         
/* 216 */         int nread = SpeechService.this.recorder.read(buffer, 0, buffer.length);
/*     */         
/* 218 */         if (this.paused) {
/*     */           continue;
/*     */         }
/*     */         
/* 222 */         if (this.reset) {
/* 223 */           SpeechService.this.recognizer.reset();
/* 224 */           this.reset = false;
/*     */         } 
/*     */         
/* 227 */         if (nread < 0) {
/* 228 */           throw new RuntimeException("error reading audio buffer");
/*     */         }
/* 230 */         if (SpeechService.this.recognizer.acceptWaveForm(buffer, nread)) {
/* 231 */           String result = SpeechService.this.recognizer.getResult();
/* 232 */           SpeechService.this.mainHandler.post(() -> this.listener.onResult(result));
/*     */         } else {
/* 234 */           String partialResult = SpeechService.this.recognizer.getPartialResult();
/* 235 */           SpeechService.this.mainHandler.post(() -> this.listener.onPartialResult(partialResult));
/*     */         } 
/*     */         
/* 238 */         if (this.timeoutSamples != -1) {
/* 239 */           this.remainingSamples -= nread;
/*     */         }
/*     */       } 
/*     */       
/* 243 */       SpeechService.this.recorder.stop();
/*     */       
/* 245 */       if (!this.paused)
/*     */       {
/* 247 */         if (this.timeoutSamples != -1 && this.remainingSamples <= 0) {
/* 248 */           SpeechService.this.mainHandler.post(() -> this.listener.onTimeout());
/*     */         } else {
/* 250 */           String finalResult = SpeechService.this.recognizer.getFinalResult();
/* 251 */           SpeechService.this.mainHandler.post(() -> this.listener.onFinalResult(finalResult));
/*     */         } 
/*     */       }
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\User\Desktop\vosk-android-lib\classes.jar!\org\vosk\android\SpeechService.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */