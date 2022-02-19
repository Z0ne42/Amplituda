package linc.com.amplituda;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;


final class FileManager {

    static final String AMPLITUDA_INTERNAL_TEMP = "amplituda_internal_temp";
    private final Resources resources;
    private final String cache;

    FileManager(final Context context) {
        resources = context.getResources();
        cache = context.getCacheDir().getPath() + File.separator;
    }

    /**
     * Delete local storage file
     */
    synchronized void deleteFile(final File file) {
        if(file != null && file.exists()) {
            file.delete();
        }
    }

    /**
     * Copy res/raw file to local storage
     * @param resource - res/raw file id
     * @return raw file from local storage
     */
    synchronized File getRawFile(final int resource, final AmplitudaProgressListener listener) {
        File temp = new File(cache, AMPLITUDA_INTERNAL_TEMP);
        try {
            InputStream inputStream = resources.openRawResource(resource);
            streamToFile(inputStream, temp, 1024 * 4, inputStream.available(), listener);
            return temp;
        } catch (Resources.NotFoundException | IOException ignored) {
            return null;
        }
    }

    /**
     * Copy audio from URL to local storage
     * @param audioUrl - audio file url
     * @return audio file from local storage
     */
    synchronized File getUrlFile(final String audioUrl, final AmplitudaProgressListener listener) {
        File temp = new File(cache, AMPLITUDA_INTERNAL_TEMP);
        try {
            URL url = new URL(audioUrl);
            URLConnection connection = url.openConnection();
            connection.connect();
            streamToFile(
                    new BufferedInputStream(url.openStream()),
                    temp,
                    1024,
                    getUrlContentLength(connection),
                    listener
            );
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return temp;
    }

    /**
     * Copy audio from Uri to local storage
     * @param audioStream - audio file stream
     * @return audio file from local storage
     */
    synchronized File getUriFile(final InputStream audioStream, final AmplitudaProgressListener listener) {
        File temp = new File(cache, AMPLITUDA_INTERNAL_TEMP);
        try {
            streamToFile(audioStream, temp, 1024 * 4, audioStream.available(), listener);
            return temp;
        } catch (Resources.NotFoundException | IOException ignored) {
            return null;
        }
    }

    /**
     * Copy audio from Uri to local storage
     * @param audioByteArray - audio file stream
     * @return audio file from local storage
     */
    synchronized File getByteArrayFile(final byte[] audioByteArray, final AmplitudaProgressListener listener) {
        File temp = new File(cache, AMPLITUDA_INTERNAL_TEMP);
        try (FileOutputStream outputStream = new FileOutputStream(temp)) {
            listener.onProgressInternal(0);
            outputStream.write(audioByteArray);
            listener.onProgressInternal(100);
            return temp;
        } catch (IOException ignored) {
            return null;
        }
    }

    /**
     * Copy audio from URL to local storage
     * @param inputStream - audio file input stream
     * @param temp - cache file to which the stream will be written
     * @param bufferSize - copy operation buffer size
     */
    private void streamToFile(
            final InputStream inputStream,
            final File temp,
            final int bufferSize,
            final long contentLength,
            final AmplitudaProgressListener listener
    ) {
        try {
            OutputStream fos = new FileOutputStream(temp);
            byte[] buffer = new byte[bufferSize];
            int read;
            int bytesWritten = 0, progress = 0;

            while ((read = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
                bytesWritten += read;

                if(listener != null && contentLength > 0) {
                    int current_progress = (int) (((float) bytesWritten / (float) contentLength) * 100.0);
                    if(current_progress != progress) {
                        listener.onProgressInternal(current_progress);
                        progress = current_progress;
                    }
                }
            }

            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException | NullPointerException ignored) {
            }
        }
    }

    private synchronized long getUrlContentLength(URLConnection url) {
        try {
            final HttpURLConnection urlConnection = (HttpURLConnection) url;
            urlConnection.setRequestMethod("HEAD");
            final String lengthHeaderField = urlConnection.getHeaderField("content-length");
            Long result = lengthHeaderField == null ? null : Long.parseLong(lengthHeaderField);
            return result == null || result < 0L ? -1L : result;
        } catch (Exception ignored) {
        }
        return -1L;
    }
}