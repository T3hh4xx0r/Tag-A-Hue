package com.t3hh4xx0r.tag_a_hue.activities;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.t3hh4xx0r.openhuesdk.sdk.PreferencesManager;
import com.t3hh4xx0r.openhuesdk.sdk.objects.Bulb;
import com.t3hh4xx0r.tag_a_hue.ChangeLogDialog;
import com.t3hh4xx0r.tag_a_hue.DBAdapter;
import com.t3hh4xx0r.tag_a_hue.R;
import com.t3hh4xx0r.tag_a_hue.RateMeMaybe;
import com.t3hh4xx0r.tag_a_hue.fragments.BulbFragment;
import com.t3hh4xx0r.tag_a_hue.fragments.GroupFragment;
import com.t3hh4xx0r.tag_a_hue.fragments.NewGroupFragment;

public class MainActivity extends FragmentActivity implements
		OnNavigationListener {
	ArrayList<Bulb> bulbs;
	PreferencesManager pMan;
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
	private NfcAdapter mAdapter;
	private PendingIntent mPendingIntent;
	int curNavPos;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	ActionBarDrawerToggle mDrawerToggle;
	private Spinner spinner;
	DBAdapter db;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);
		spinner = (Spinner) findViewById(R.id.spinner);
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
		pMan = new PreferencesManager(this);
		bulbs = pMan.getBulbs();

		setNavItems(-1);

		mAdapter = NfcAdapter.getDefaultAdapter(this);

		mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
				R.drawable.ic_drawer, R.string.app_name, R.string.app_name) {
			public void onDrawerClosed(View view) {
			}

			public void onDrawerOpened(View drawerView) {
			}
		};
		db = new DBAdapter(this);
		db.open();
		if (bulbs.size() > 0) {
			mDrawerLayout.setDrawerListener(mDrawerToggle);
			mDrawerList.setAdapter(new ArrayAdapter<String>(this,
					R.layout.drawer_list_item, db
							.getSwitchesForId(getIdForNavPosition(0))));

			ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
					R.layout.drawer_list_item, getNavItems(false));
			dataAdapter
					.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
			spinner.setAdapter(dataAdapter);
			spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> arg0, View v, int p,
						long arg3) {
					mDrawerList.setAdapter(new ArrayAdapter<String>(v
							.getContext(), R.layout.drawer_list_item, db
							.getSwitchesForId(getIdForNavPosition(p))));
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
				}
			});
		}

		ChangeLogDialog.show(this);
		RateMeMaybe rmm = new RateMeMaybe(this);
		rmm.setPromptMinimums(0, 0, 10, 7);
		rmm.setDialogMessage("If you like this app, please take a moment to comment and rate it on Google Play. Thank you!");
		rmm.setDialogTitle("Rate this app?");
		rmm.setPositiveBtn("Sure!");
		rmm.run();

	}

	@Override
	public boolean onNavigationItemSelected(int position, long arg1) {
		Fragment fragment = null;
		Bundle args = new Bundle();

		if (position == getNavItems(true).size() - 1) {
			fragment = new NewGroupFragment();
		} else if (position < bulbs.size()) {
			fragment = new BulbFragment();
			args.putSerializable("bulb", bulbs.get(position));
		} else {
			fragment = new GroupFragment();
			args.putString("group", getNavItems(true).get(position));
		}
		if (fragment != null) {
			fragment.setArguments(args);
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.container, fragment).commit();
		}
		curNavPos = position;
		mDrawerLayout.closeDrawer(GravityCompat.START);
		return true;
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
				.getSelectedNavigationIndex());
	}

	public void setNavItems(final int pos) {
		MainActivity.this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				getActionBar().setListNavigationCallbacks(
						new ArrayAdapter<String>(MainActivity.this
								.getActionBar().getThemedContext(),
								android.R.layout.simple_list_item_1,
								android.R.id.text1, getNavItems(true).toArray(
										new String[getNavItems(true).size()])),
						MainActivity.this);
				if (pos != -1) {
					getActionBar().setSelectedNavigationItem(pos);
				}
			}
		});
	}

	public ArrayList<String> getNavItems(boolean withCreate) {
		ArrayList<String> res = new ArrayList<String>();
		for (Bulb b : bulbs) {
			res.add(b.getName());
		}
		ArrayList<String> gNames = pMan.getGroupNames();
		for (String name : gNames) {
			res.add(name);
		}
		if (withCreate) {
			res.add("Create New Group");
		}
		return res;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		} else if (item.getItemId() == R.id.action_help) {
			showHelp();
		}
		return super.onOptionsItemSelected(item);
	}

	private void showHelp() {
		AlertDialog.Builder b = new Builder(this);
		b.setTitle("Help!");
		b.setMessage("Select a bulb/group from the dropdown. " +
				"Once selected, scan a compatible NFC tag. After agreeing to the prompt, " +
				"the app will write a small amount of data to the tag. When this tag is scanned next, " +
				"the app will toggle the state of the bulb/group the tag was associated with." +
				"\n\nTo the left in the nav drawer you can find a list of tags associated with each bulb/group." +
				"\n\n**REMEMBER**\nThe bulbs must physically be on for this to work. " +
				"The app is only able to control the software state of the bulbs.");
		b.create().show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent);
		try {
			resolveIntent(intent);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mAdapter != null) {
			mAdapter.disableForegroundDispatch(this);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
	}

	private void resolveIntent(final Intent intent) throws IOException {
		mDrawerLayout.closeDrawer(GravityCompat.START);
		final byte[] tagId = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);

		if (tagId != null) {
			final String id = byteArrayToHexString(tagId);
			AlertDialog.Builder b = new Builder(this);
			b.setMessage("Tag Found");
			b.setMessage("Please keep your device connected to the tag until the process is over.\n\nAssociate this tag with Bulb \""
					+ getNavItems(true).get(curNavPos) + "\"?");
			b.setPositiveButton("Yes", new OnClickListener() {
				@Override
				public void onClick(DialogInterface d, int p) {
					try {
						finishTag(intent, id);
					} catch (IOException e) {
						Toast.makeText(
								MainActivity.this,
								"Unable to connect to tag. Be sure it is still touching the scanner.",
								Toast.LENGTH_LONG).show();
					}
					d.dismiss();
				}
			});
			b.setNegativeButton("No", new OnClickListener() {
				@Override
				public void onClick(DialogInterface d, int arg1) {
					d.dismiss();
				}
			});

			b.create().show();
		}
	}

	public void finishTag(Intent intent, String id) throws IOException {
		final Object[] verifyResult = verifyTag(intent);
		if (!((Boolean) verifyResult[0])) {
			Toast.makeText(getApplicationContext(), ((String) verifyResult[1]),
					Toast.LENGTH_LONG).show();
			return;
		} else {
			if (((String) verifyResult[1]).equals("DONE")) {
				db.insertSwitch(id, bulbs.get(curNavPos).getNumber());
				mDrawerList.setAdapter(new ArrayAdapter<String>(this,
						R.layout.drawer_list_item, db.getSwitches()));
			} else if (((String) verifyResult[1]).equals("READY")) {
				Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
				if (!writeTag(tag)) {
					Toast.makeText(getApplicationContext(),
							"Failed to write to tag. You may try again.",
							Toast.LENGTH_SHORT).show();
					return;
				} else {
					db.insertSwitch(id, getIdForNavPosition(curNavPos));
					mDrawerList.setAdapter(new ArrayAdapter<String>(this,
							R.layout.drawer_list_item, db.getSwitches()));
				}
			}
		}
	}

	private String getIdForNavPosition(int p) {
		if (p == getNavItems(true).size()) {
			return null;
		} else if (p < bulbs.size()) {
			return bulbs.get(p).getNumber();
		} else {
			return getNavItems(true).get(p);
		}
	}

	static String byteArrayToHexString(byte[] inarray) {
		int i, j, in;
		String[] hex = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A",
				"B", "C", "D", "E", "F" };
		String out = "";

		for (j = 0; j < inarray.length; ++j) {
			in = (int) inarray[j] & 0xff;
			i = (in >> 4) & 0x0f;
			out += hex[i];
			i = in & 0x0f;
			out += hex[i];
		}
		return out;
	}

	private Object[] verifyTag(Intent intent) {
		Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		Ndef ndef = Ndef.get(tag);
		if (ndef != null) {
			try {
				ndef.connect();
				if (!ndef.isWritable()) {
					if (!intent.getType().equals("tagahue/switch")) {
						ndef.close();
						return new Object[] { false, "Key not writable." };
					} else {
						ndef.close();
						return new Object[] { true, "DONE" };
					}
				} else {
					ndef.close();
					return new Object[] { true, "READY" };
				}
			} catch (IOException e) {
				return new Object[] { false, "Error reading key." };
			}
		} else {
			if (NdefFormatable.get(tag) == null) {
				return new Object[] { false, "Error reading key." };
			} else {
				byte[] blank = new byte[0];
				NdefRecord blankRecord = new NdefRecord(NdefRecord.TNF_EMPTY,
						blank, blank, blank);
				NdefFormatable ndefF = NdefFormatable.get(tag);
				try {
					ndefF.connect();
					NdefMessage message = new NdefMessage(
							new NdefRecord[] { blankRecord });
					ndefF.format(message);
					ndefF.close();
					return new Object[] { true, "READY" };
				} catch (IOException e) {
					e.printStackTrace();
					return new Object[] { false, "Error reading key." };
				} catch (FormatException e) {
					e.printStackTrace();
					return new Object[] { false, "Error reading key." };
				}
			}
		}
	}

	private boolean writeTag(Tag tag) {
		NdefMessage message;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
			message = new NdefMessage(NdefRecord.createMime("tagahue/switch",
					new byte[0]));
		} else {
			message = new NdefMessage(new NdefRecord[] { createMimeRecord(
					"tagahue/switch", new byte[0]) });
		}

		try {
			Ndef ndef = Ndef.get(tag);
			if (ndef != null) {
				ndef.connect();
				if (!ndef.isWritable()) {
					displayMessage("Read-only tag.");
					return false;
				} else {
					int size = message.toByteArray().length;
					if (ndef.getMaxSize() < size) {
						displayMessage("Tag doesn't have enough free space.");
						return formatTag(tag, message);
					}
					ndef.writeNdefMessage(message);
					displayMessage("Tag written successfully.");
					return true;
				}
			} else {
				return formatTag(tag, message);
			}
		} catch (Exception e) {
			e.printStackTrace();
			displayMessage("Failed to write tag : " + e.getMessage());
		}
		return false;
	}

	public NdefRecord createMimeRecord(String mimeType, byte[] payload) {
		byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
		NdefRecord mimeRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
				mimeBytes, new byte[0], payload);
		return mimeRecord;
	}

	private void displayMessage(String m) {
		Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
	}

	private boolean formatTag(Tag tag, NdefMessage m) {
		NdefFormatable format = NdefFormatable.get(tag);
		if (format != null) {
			try {
				format.connect();
				format.format(m);
				displayMessage("Tag written successfully!");
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				displayMessage("Unable to format tag to NDEF.");
				return false;
			} catch (FormatException e) {
				e.printStackTrace();
				return false;
			}
		} else {
			displayMessage("Tag doesn't appear to support NDEF format.");
			return false;
		}
	}

}
