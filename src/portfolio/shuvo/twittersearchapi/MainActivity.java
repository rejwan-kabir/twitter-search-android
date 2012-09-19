package portfolio.shuvo.twittersearchapi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import portfolio.shuvo.twittersearchapi.pojo.Response;
import portfolio.shuvo.twittersearchapi.pojo.Tweet;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

// TODO : read - http://www.google.com/events/io/2010/sessions/world-of-listview-android.html
public class MainActivity extends Activity {
	private String jsonUrl = "http://search.twitter.com/search.json?"
			+ "q=from%3Agoogle&result_type=recent&rpp=5&page=";
	private int count = 0;
	private ListView listView;
	private ArrayList<Tweet> tweets = new ArrayList<Tweet>();
	private ArrayAdapter<Tweet> listAdapter;
	private String title;
	private String username;
	private Bitmap userImage;
	private LayoutInflater layoutInflater;

	private static final String TAG = "TwitterApp";
	private static final DateFormat INCOMING_DATE_FORMAT = new SimpleDateFormat(
			"EEE, dd MMM yyyy kk:mm:ss zzz", Locale.getDefault());
	private static final DateFormat OUTGOING_DATE_FORMAT = new SimpleDateFormat(
			"EEE, dd MMM yyyy KK:mm:ss a", Locale.getDefault());

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.listView = (ListView) findViewById(R.id.tweetListView);
		this.listAdapter = new TweetAdapter(this, R.layout.tweet_unit,
				this.tweets);
		this.listView.setAdapter(listAdapter);
		this.layoutInflater = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		this.loadTweet();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	private void loadTweet() {
		ConnectivityManager connectivityManager = (ConnectivityManager) this
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {
			new LoadTask(this).execute(this.jsonUrl + ++count);
		} else {
			Toast.makeText(this, "No Internet Connectivity !",
					Toast.LENGTH_LONG).show();
		}
	}

	public void buttonClicked(View view) {
		this.loadTweet();
	}

	static class TweetHolder {
		ImageView userImageView;
		TextView usernameTextView;
		TextView tweet_bodyTextView;
		TextView created_atTextView;
	}

	private class TweetAdapter extends ArrayAdapter<Tweet> {
		private ArrayList<Tweet> tweet;

		public TweetAdapter(Context context, int viewResourceId,
				ArrayList<Tweet> objects) {
			super(context, viewResourceId, objects);
			this.tweet = objects;
		}

		// The correct way
		// @Override
		// public View getView(int position, View convertView, ViewGroup parent)
		// {
		// if (convertView == null) {
		// convertView = ((LayoutInflater) MainActivity.this
		// .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
		// .inflate(R.layout.tweet_unit, parent, false);
		// }
		// Tweet singleTweet = this.tweet.get(position);
		// if (singleTweet != null) {
		// ImageView userImageView = (ImageView) convertView
		// .findViewById(R.id.userImageView);
		// TextView usernameTextView = (TextView) convertView
		// .findViewById(R.id.usernameTextView);
		// TextView tweet_bodyTextView = (TextView) convertView
		// .findViewById(R.id.tweet_bodyTextView);
		// TextView created_atTextView = (TextView) convertView
		// .findViewById(R.id.created_atTextView);
		// if (usernameTextView != null) {
		// usernameTextView.setText(username);
		// }
		// if (tweet_bodyTextView != null) {
		// tweet_bodyTextView.setText(singleTweet.getText());
		// }
		// if (created_atTextView != null) {
		// created_atTextView.setText(singleTweet.getCreated_at());
		// }
		// if (userImageView != null) {
		// userImageView.setImageBitmap(userImage);
		// }
		// }
		// return convertView;
		// }

		// the fastest way
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Tweet singleTweet = this.tweet.get(position);
			TweetHolder tweetHolder;
			if (convertView == null) {
				convertView = MainActivity.this.layoutInflater.inflate(
						R.layout.tweet_unit, parent, false);
				tweetHolder = new TweetHolder();
				tweetHolder.userImageView = (ImageView) convertView
						.findViewById(R.id.userImageView);
				tweetHolder.usernameTextView = (TextView) convertView
						.findViewById(R.id.usernameTextView);
				tweetHolder.tweet_bodyTextView = (TextView) convertView
						.findViewById(R.id.tweet_bodyTextView);
				tweetHolder.created_atTextView = (TextView) convertView
						.findViewById(R.id.created_atTextView);
				convertView.setTag(tweetHolder);
			} else {
				tweetHolder = (TweetHolder) convertView.getTag();
			}
			tweetHolder.usernameTextView.setText(MainActivity.this.username);
			tweetHolder.tweet_bodyTextView.setText(singleTweet.getText());
			try {
				tweetHolder.created_atTextView.setText(OUTGOING_DATE_FORMAT
						.format(INCOMING_DATE_FORMAT.parse(singleTweet
								.getCreated_at())));
			} catch (ParseException e) {
				tweetHolder.created_atTextView.setText("");
			}
			tweetHolder.userImageView
					.setImageBitmap(MainActivity.this.userImage);
			return convertView;
		}
	}

	private class LoadTask extends AsyncTask<String, Integer, ArrayList<Tweet>> {
		private final HttpClient httpClient = new DefaultHttpClient();
		private HttpResponse httpResponse;
		private BufferedReader bufferedReader;
		private Gson gson;
		private ArrayList<Tweet> newTweets;
		private Response response;
		private String username;
		private Bitmap userImage;
		private String title;
		private ProgressDialog progressDialog;
		private Context context;

		public LoadTask(Context context) {
			this.context = context;
			this.progressDialog = new ProgressDialog(this.context);
			this.progressDialog.setCancelable(true);
			this.progressDialog.setMessage("Loading ...");
			this.progressDialog.setIndeterminate(true);
		}

		@Override
		protected void onPreExecute() {
			this.progressDialog.show();
		}

		@Override
		protected ArrayList<Tweet> doInBackground(String... url) {
			try {
				this.httpResponse = this.httpClient
						.execute(new HttpGet(url[0]));
				this.bufferedReader = new BufferedReader(new InputStreamReader(
						this.httpResponse.getEntity().getContent()));
				this.gson = new Gson();
				this.response = gson.fromJson(bufferedReader, Response.class);
				this.newTweets = this.response.getTweets();
				if (this.newTweets.size() != 0) {
					if (this.username == null) {
						this.username = this.newTweets.get(0)
								.getFrom_user_name();
					}
					if (this.userImage == null) {
						this.userImage = BitmapFactory.decodeStream(new URL(
								this.newTweets.get(0).getProfile_image_url())
								.openConnection().getInputStream());
					}
					if (this.title == null) {
						this.title = "@" + this.newTweets.get(0).getFrom_user();
					}
					return this.newTweets;
				}
			} catch (Exception e) {
				Log.e(TAG, "exception thrown ", e);
			}
			return null;
		}

		@Override
		protected void onPostExecute(ArrayList<Tweet> result) {
			super.onPostExecute(result);
			if (result != null) {
				if (MainActivity.this.title == null) {
					MainActivity.this.username = this.username;
					MainActivity.this.userImage = this.userImage;
					MainActivity.this.title = this.title;
					MainActivity.this.setTitle(this.title);
				}
				MainActivity.this.tweets.addAll(result);
				Log.d(TAG, "Showing : " + MainActivity.this.tweets.size());
				// notifyDataSetChanged() causes the ListView to rebuild its
				// entire View hierarchy is very slow if you are calling it
				// frequently (i.e. once every second).
				// http://stackoverflow.com/questions/5846385/how-to-update-android-listview-with-dynamic-data-in-real-time
				MainActivity.this.listAdapter.notifyDataSetChanged();
			} else {
				Toast.makeText(MainActivity.this, "No more Tweets !",
						Toast.LENGTH_SHORT).show();
			}
			if (this.progressDialog != null && this.progressDialog.isShowing()) {
				this.progressDialog.dismiss();
			}
		}
	}
}
