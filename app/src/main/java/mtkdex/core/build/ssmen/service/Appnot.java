package mtkdex.core.build.ssmen.service;

import android.content.Context;
import android.os.AsyncTask;

import com.v2ray.ang.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;


public class Appnot extends AsyncTask<String, String, String>
{
	private static final String URL = "https://gitlab.com/vollam/appkill/-/raw/main/appkill";
	private Context context;
	private OnFinishListener listener;
	public interface OnFinishListener
	{
		void onFinish(boolean isDestroy, String message);
	}
	public Appnot(Context context)
	{
		this.context = context;
	}
	public void setListener(OnFinishListener OnFinishListener)
	{
		listener = OnFinishListener;
	}
	public void start()
	{
		execute(URL);
	}
	@Override
	protected String doInBackground(String[] p1)
	{
		try {
			URL mURL = new URL(p1[0]);
			HttpURLConnection con = (HttpURLConnection)mURL.openConnection();
			con.connect();

			StringBuilder sb = new StringBuilder();
			Reader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
			char[] buf = new char[1024];
			while (true) {
				int read = reader.read(buf);
				if (read <= 0) {
					break;
				}
				sb.append(buf, 0, read);
			}
			return sb.toString();
		} catch (Exception e) {

		}
		// TODO: Implement this method
		return null;
	}

	@Override
	protected void onPostExecute(String result)
	{
		if (result != null) {
			String appName = context.getString(R.string.app_name);
			String packageName = context.getPackageName();
			try {
				JSONObject js = new JSONObject(result);
				if (js.has(appName)) {
					boolean isDestroy = js.getBoolean(appName);
					listener.onFinish(isDestroy, js.getString("Message"));
				} else if (js.has(packageName)) {
					boolean isDestroy = js.getBoolean(packageName);
					listener.onFinish(isDestroy, js.getString("Message"));
				}
			} catch (Exception e) {

			}
		}
		// TODO: Implement this method
		super.onPostExecute(result);
	}

}
