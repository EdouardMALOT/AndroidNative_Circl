package io.circl.circl;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * Created by edouard on 08/06/16.
 */
public class EventActivity extends Activity {

    Bitmap EventBitmap = null;
    ImageView ImageEventView;
    String MapCamera;
    int ModeAffichage;
    String Lien = "";

    private GoogleApiClient client;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Display Layout
        setContentView(R.layout.displayevent);

        ///Get the parsed datas
        Intent GetIntent = getIntent();
        Bundle GetBundle = GetIntent.getExtras();

        String Titre = GetBundle.getString("Titre");
        String Description = GetBundle.getString("Description");
        String Image = MapsActivity.Website + "ImgEvent/" + GetBundle.getString("Image").replace(" ", "%20");
        String Prix = GetBundle.getString("Prix");
        String Auteur = GetBundle.getString("Auteur");
               Lien = GetBundle.getString("Lien");
        String Horraires = GetBundle.getString("Horraires");


        MapCamera = GetBundle.getString("CameraPositionSaved");
        ModeAffichage = GetBundle.getInt("ModeAffichage");

        //Set Titre
        TextView TitreView = (TextView) findViewById(R.id.TitreEvent);
        TitreView.setText(Titre);

        //Set Description
        TextView DescriptionView = (TextView) findViewById(R.id.DescriptionEvent);
        DescriptionView.setText(Description);

        //Set Image
        new TaskGetPicture().execute(Image);

        //Set horraires
        TextView HorrairesView = (TextView) findViewById(R.id.horraires);
        HorrairesView.setText(Horraires);

        //Set Prix
        TextView PriceView = (TextView) findViewById(R.id.prix);

            //Update du prix
            if( Prix.equals("" ) )                          //Si rienon ne met rien
            {
                //On ne mets rien dans le text
                PriceView.setText(" ");

                //On efface le logo PRIX
                ImageView LogoPrix = (ImageView) findViewById(R.id.logoprix);
                LogoPrix.setVisibility(View.INVISIBLE);

            }else if(Prix.equals("0"))                      //Si 0 on indique gratuit
            {
                PriceView.setText("Gratuit");

            }else {
                PriceView.setText(Prix+"€");                //Sinon on affiche la valeur
            }

        //Auteur
        TextView AuthorView = (TextView) findViewById(R.id.Author);
        AuthorView.setText("Proposé par : "+Auteur);


        //When button is Clicked, go back to Maps
        //---------------------------------------
        Button BP_retour = (Button) findViewById(R.id.back_from_event);
        BP_retour.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //Save Zoom Value (Map will use it)
                SharedPreferences.Editor SaveData = getSharedPreferences(MapsActivity.AppSharedName, MODE_PRIVATE).edit();

                SaveData.putString("CameraPositionSaved", MapCamera);
                SaveData.putInt("ModeAffichage", ModeAffichage);

                SaveData.commit();

                Intent MapIntent = new Intent(getApplicationContext(), MapsActivity.class);
                startActivity(MapIntent);
            }
        });


        //When Circl is Clicked show pop-up
        //----------------------------------
        Button BP_Share = (Button) findViewById(R.id.share_btn);
        BP_Share.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //Dialog box
                AlertDialog.Builder alert = new AlertDialog.Builder(EventActivity.this);
                alert.setTitle("Partagez");
                alert.setMessage("Cet évènement Circl vous plait, partagez vos bons plans et faites grandir la communautée.");

                // Add the buttons
                alert.setNegativeButton("Retour", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });


                alert.setPositiveButton("Partager", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        //Creation du partage en cours
                        Toast.makeText(getApplicationContext(), getString(R.string.ShareMsg), Toast.LENGTH_SHORT).show();
                        ShareScreen();
                    }
                });

                alert.show();

            }
        });



        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Event Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://io.circl.circl/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Event Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://io.circl.circl/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //                  GetEvent                    //
    //             Get Ig and display it            //
    //                                              //
    //////////////////////////////////////////////////
    class TaskGetPicture extends AsyncTask<String, String, String> {

        ImageView ImageEventView;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ImageEventView = (ImageView) findViewById(R.id.image_event);
            ImageEventView.setImageResource(R.drawable.imgchargement);

            //Si le lien existe on crée le clic !
            //-----------------------------------
            if(Lien.length() > 3) {
                ImageEventView.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        //Ouverture du lien
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(Lien));
                        startActivity(i);
                    }
                });
            }

        }

        @Override
        protected String doInBackground(String... strings) {

            int TryCount = 0;
            HttpURLConnection connection = null;

            while (TryCount < MapsActivity.Nbhttpretry) {
                try {
                    URL url = new URL(strings[0]);

                    connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();

                    InputStream input = connection.getInputStream();
                    EventBitmap = BitmapFactory.decodeStream(input);

                    return "OK";
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {

                    TryCount++;
                    if (connection != null)
                        connection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result != null) {

                ImageEventView.setImageBitmap(EventBitmap);
                scaleImage(ImageEventView);


            } else {
                //Error

            }
        }
    }

    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //                  scaleImage                  //
    //                                              //
    //                                              //
    //////////////////////////////////////////////////
    private void scaleImage(ImageView view) {

        Drawable drawing = view.getDrawable();
        if (drawing == null) {
            return;
        }

        //Get Image Size
        Bitmap bitmap = ((BitmapDrawable) drawing).getBitmap();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        //compute Displayed size
        int bounding_x = view.getWidth();
        int bounding_y =(int)( (double)(height)/(double)(width)*(double)(bounding_x));

        //Check If Height size do not exced the limit
        int ImgMaxSize = (int)(0.45 * (double)(((View)view.getParent()).getHeight()));
        if(bounding_y >  ImgMaxSize ) {
            bounding_y  = ImgMaxSize;
            //bounding_x =(int)( (double)(width)/(double)(height)*(double)(bounding_y));
        }

        //Compute Scale factors
        float xScale = ((float) bounding_x) / width;
        float yScale = ((float) bounding_y) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(xScale, yScale);

        //Apply scale on picture
        Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        width = scaledBitmap.getWidth();
        height = scaledBitmap.getHeight();
        BitmapDrawable result = new BitmapDrawable(getApplicationContext().getResources(), scaledBitmap);

        //Apply Img on View
        view.setImageDrawable(result);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.width = width;
        params.height = height;
        view.setLayoutParams(params);
    }


    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //                  ShareScreen                 //
    //                                              //
    //                                              //
    //////////////////////////////////////////////////
    private void ShareScreen()
    {
        try {

            //Take a screenshot
            //-----------------
            View rootView = findViewById(android.R.id.content).getRootView();
            rootView.setDrawingCacheEnabled(true);
            Bitmap bitmap =rootView.getDrawingCache();


            //Ask permission to WRITE if not already asked
            //--------------------------------------------
            verifyStoragePermissions(EventActivity.this);

            //Save picture on device
            //----------------------
            File filePath = null;
            filePath = SaveBitmap(bitmap);

            //Make Intent
            //-----------
            Intent ShareIntent = new Intent(Intent.ACTION_SEND);

            ShareIntent.putExtra(Intent.EXTRA_TEXT, "Evènement proposé par l'Appli Circl:");
            ShareIntent.setType("text/plain");
            ShareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath.toString())));
            ShareIntent.setType("image/jpeg");

            startActivity(ShareIntent);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //           verifyStoragePermissions           //
    //                                              //
    //                                              //
    //////////////////////////////////////////////////
    public static void verifyStoragePermissions(Activity activity) {

        // Check if we have write permission
        int ReadPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        int WritePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if ( ! ( (WritePermission == PackageManager.PERMISSION_GRANTED) &&  (ReadPermission == PackageManager.PERMISSION_GRANTED ) ) )
        {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    //////////////////////////////////////////////////
    //                                              //
    //                                              //
    //           Save Bipmap on Phone               //
    //                                              //
    //                                              //
    //////////////////////////////////////////////////
    public static File SaveBitmap(Bitmap bmp) throws IOException {

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

        File f = new File(Environment.getExternalStorageDirectory().toString() + File.separator + "Capture.jpg");

        f.createNewFile();
        FileOutputStream fo = new FileOutputStream(f);
        fo.write(bytes.toByteArray());

        fo.close();

        return f;
    }
}
