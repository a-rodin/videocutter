package com.ffcut;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Scanner;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Класс, реализующий обрезание видео и наложение на него звука
 * @author <a href="mailto:rodin.alexander@gmail.com">Александр Родин</a>
 */
public class FFCut {
	
	/**
	 * Listener для отслеживания состояния процесса обработки
	 */
	public static interface Listener {
		/**
		 * Вызывается при изменении прогресса
		 * @param progress прогресс в процентах (от 0 до 100)
		 */
		void onProgress(double progress);
		/**
		 * Вызывается при успешном завершении обработки
		 */
		void onFinish();
		/**
		 * Вызывается, если при обработке произошла какая-либо ошибка
		 */
		void onFail();
	}
	
	/**
	 * Выбрасываемое при ошибке инициализации исключение
	 */
	public static class InitError extends java.lang.Error {
		private static final long serialVersionUID = 5914806310985503745L;
		public InitError() {
			super("Cannot initialize ffcut");
		}
	}
	
	/**
	 * Необходимо вызывать **при первом запуске** приложения, в последующие разы инициализация не нужна
	 * @param context контекст приложения
	 */
	public static void init(Context context) throws InitError {
		String executablePath = "/data/data/" + context.getPackageName() + "/ffmpeg";
		
		Log.d("FFCut", "initializing...");
		try {
			InputStream ffcutSrc = context.getAssets().open("ffmpeg");
			FileOutputStream ffcutDest = new FileOutputStream(executablePath);
			Log.d("FFCut", "copying executable...");
			byte [] buf = new byte[1024];
			while (ffcutSrc.read(buf) != -1) {
				ffcutDest.write(buf);
			}
			ffcutDest.close();
			ffcutSrc.close();
			Log.d("FFCut", "executable is copyed, applying permissions...");
			Process chmod =  Runtime.getRuntime().exec("/system/bin/chmod 755 " + executablePath);
			chmod.waitFor();
			Log.d("FFCut", "ffcut is initialized");
		} catch (Exception e) {
			Log.d("FFCut", "ffcut initialization is failed, " + e.getClass().getName() + ": " + e.getMessage());
			throw new InitError();
		}
	}
	
	
	/**
	 * Обработка видео
	 * @param context контекст
	 * @param srcPath путь к файлу, который необходимо обработать
	 * @param startPos начало фрагмента, который необходимо вырезать, в секундах
	 * @param endPos конец фрагмента, который необходимо вырезать, в секундах
	 * @param audioPath путь к файлу со звуком или {@code null}
	 * @param listener экземпляр {@link Listener}, отслеживающий процесс обработки
	 * Если аудио длиннее вырезаемого фрагмента, от него берется только начало соответствующей длины.
	 */
	public static void process(Context context, String srcVideoPath, String srcAudioPath, String destPath, final double startPos, final double endPos, final Listener listener) {
		AsyncTask<String, Double, Integer> task = new AsyncTask<String, Double, Integer>() {
			@Override
			protected Integer doInBackground(String... params) {
				String cmd = params[0];
				Log.d("FFCut", "running command " + cmd);
				try {
					Process ffcut =  Runtime.getRuntime().exec(cmd);
					InputStream output = ffcut.getInputStream();
					InputStream error = ffcut.getErrorStream();
					Scanner scanner = new Scanner(output);
					Scanner errorScanner = new Scanner(error);
					while (scanner.hasNextDouble()) {
						publishProgress(new Double(scanner.nextDouble()));
					}
					while (errorScanner.hasNextLine()) {
						Log.d("FFCut", "ffmpeg: "  + errorScanner.nextLine());
					}
					return new Integer(ffcut.waitFor());
				} catch (Exception e) {
					Log.d("FFCut", "exception " + e.getClass().getName() + ": " + e.getMessage());
					return new Integer(200);
				}
			}
			
			@Override
			protected void onProgressUpdate(Double... values) {
				double progress = values[0].doubleValue() * 100.0 / (endPos - startPos);
				Log.d("FFCut", "progress: " + progress + "%");
				listener.onProgress(progress);
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				Log.d("FFCut", "ffmpeg is finished with code " + result.intValue());
				if (result.intValue() == 0) {
					listener.onFinish();
				} else {
					listener.onFail();
				}
			}
			
			@Override
			protected void onCancelled() {
				listener.onFail();
			}
		};
		
		String cmd = 
				"/data/data/" + context.getPackageName() + "/ffmpeg" +
				" -ss " + startPos + " -i " + srcVideoPath.replace(" ", "\\ ") + 
				(srcAudioPath == null ? "" : (" -i " + srcAudioPath.replace(" ", "\\ "))) +
				" -t " + (endPos - startPos) + 
				" -map 0:0 -map 1:0 -vcodec copy -acodec copy -sameq -y " +
				destPath.replace(" ", "\\ ");
		task.execute(cmd);
	}
	
	private FFCut() {}
}
