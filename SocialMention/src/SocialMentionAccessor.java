import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

public class SocialMentionAccessor {
	private static final String url = "http://socialmention.com/";

	@SuppressWarnings("serial")
	public static void main(final String[] args) {

		final List<String> queryTerms = new ArrayList<String>() {
			{
				add("iphone");
				add("apps");
			}
		};
		final List<String> searchType = new ArrayList<String>() {
			{
				add("blogs");
				add("microblogs");
			}
		};
		final String location = "usa";

		try {
			final String urlString = SocialMentionAccessor.createQueryURL(queryTerms, searchType, location);
			final String socialMentionJSONResponse = SocialMentionAccessor.getSocialMentionJSONResponse(urlString);

			final SocialMentionData collectedInfo = new Gson().fromJson(socialMentionJSONResponse, SocialMentionData.class);
			System.out.println("done");

		} catch (final MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final SocialMentionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static String getSocialMentionJSONResponse(final String urlString) throws IOException, SocialMentionException {
		HttpURLConnection conn;
		final URL url = new URL(urlString);
		conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setUseCaches(false);
		conn.setAllowUserInteraction(false);
		conn.connect();

		try {
			final int responseCode = conn.getResponseCode();
			if (responseCode == 200 || responseCode == 201) {
				final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				final StringBuilder sb = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}
				reader.close();
				return sb.toString();
			} else {
				throw new SocialMentionException("server status code: " + responseCode);
			}
		} finally {
			conn.disconnect();
		}
	}

	/**
	 * Build the URL-String to query the Social Mention API
	 * 
	 * @see https://code.google.com/p/socialmention-api/wiki/APIDocumentation
	 * 
	 * @param queryTerms
	 * @param responseFormat
	 * @param searchType
	 * @param location
	 * 
	 * @return URL string
	 * @throws MalformedURLException
	 */
	private static String createQueryURL(final List<String> queryTerms, final List<String> searchType, final String location)
			throws MalformedURLException {
		final StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("search?");

		// append queryTerms
		if (queryTerms == null || queryTerms.isEmpty()) {
			throw new MalformedURLException("no query term was given");
		}
		sb.append("q=");
		for (int index = 0; index < queryTerms.size(); index++) {
			sb.append(queryTerms.get(index));
			if (index < queryTerms.size() - 1) {
				sb.append("+");
			}
		}

		// append response format (JSON)
		sb.append("&f=");
		sb.append("json");

		// append search type
		sb.append("&");
		if (searchType == null || searchType.isEmpty()) {
			sb.append("t=");
			sb.append("all");
		} else if (searchType.size() == 1) {
			sb.append("t=");
			sb.append(searchType.get(0));
		} else {
			for (int index = 0; index < searchType.size(); index++) {
				sb.append("t[]=");
				sb.append(searchType.get(index));
				sb.append("&");
			}
		}

		// append location string
		if ((location == null || location == "") && searchType.contains("events")) {
			throw new MalformedURLException("location string is mandatory for search type 'events'");
		}
		if (!(location == null || location == "")) {
			sb.append("l=");
			sb.append(location);
		}

		return sb.toString();
	}

}