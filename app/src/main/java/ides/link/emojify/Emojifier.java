package ides.link.emojify;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

/**
 * Created by Eman on 9/12/2017.
 */

public class Emojifier {
    private static final String LOG_TAG = Emojifier.class.getSimpleName();
    private static final float EMOJI_SCALE_FACTOR = .9f;
    private static final double SMILING_PROB_THRESHOLD = .15;
    private static final double EYE_OPEN_PROB_THRESHOLD = .5;

    /**
     * Method for detecting faces in a bitmap.
     *
     * @param context The application context.
     * @param picture The picture in which to detect the faces.
     */
    static Bitmap detectFacesAndOverlayEmoji(Context context, Bitmap picture) {

        // Create the face detector, disable tracking and enable classifications
        FaceDetector detector = new FaceDetector.Builder(context)
                .setTrackingEnabled(false)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        // Build the frame
        Frame frame = new Frame.Builder().setBitmap(picture).build();

        // Detect the faces
        SparseArray<Face> faces = detector.detect(frame);

        // Log the number of faces
        Log.d(LOG_TAG, "detectFaces: number of faces = " + faces.size());

        // If there are no faces detected, show a Toast message
        if (faces.size() == 0) {
            Toast.makeText(context, R.string.no_faces_message, Toast.LENGTH_SHORT).show();
        }

        for (int i = 0; i < faces.size(); ++i) {
            Face face = faces.valueAt(i);
            int imageId = whichEmoji(face);

            Bitmap emoji = BitmapFactory.decodeResource(context.getResources(), imageId);

            return addBitmapToFace(picture, emoji, face);
        }

        // Release the detector
        detector.release();
        return null;
    }

    /**
     * Method for logging the classification probabilities.
     *
     * @param face The face to get the classification probabilities.
     */
    private static int whichEmoji(Face face) {
        // Log all the probabilities
        float smilling = face.getIsSmilingProbability();
        float rightOpen = face.getIsRightEyeOpenProbability();
        float liftOpen = face.getIsLeftEyeOpenProbability();
        boolean isSmilling = false, isRightOpen = false, isLiftOpen = false;
        if (smilling >= SMILING_PROB_THRESHOLD) {
            isSmilling = true;
        }
        if (rightOpen >= EYE_OPEN_PROB_THRESHOLD) {
            isRightOpen = true;
        }
        if (liftOpen >= EYE_OPEN_PROB_THRESHOLD) {
            isLiftOpen = true;
        }

        if (isSmilling) {
            if (isRightOpen) {
                if (isLiftOpen) {
                    return R.drawable.smile;
                } else {
                    return R.drawable.leftwink;
                }
            } else {
                if (isLiftOpen) {
                    return R.drawable.rightwink;
                } else {
                    return R.drawable.closed_smile;
                }
            }
        } else {
            if (isRightOpen) {
                if (isLiftOpen) {
                    return R.drawable.frown;
                } else {
                    return R.drawable.leftwinkfrown;
                }
            } else {
                if (isLiftOpen) {
                    return R.drawable.rightwinkfrown;
                } else {
                    return R.drawable.closed_frown;
                }
            }
        }
    }

    /**
     * Combines the original picture with the emoji bitmaps
     *
     * @param backgroundBitmap The original picture
     * @param emojiBitmap      The chosen emoji
     * @param face             The detected face
     * @return The final bitmap, including the emojis over the faces
     */
    private static Bitmap addBitmapToFace(Bitmap backgroundBitmap, Bitmap emojiBitmap, Face face) {

        // Initialize the results bitmap to be a mutable copy of the original image
        Bitmap resultBitmap = Bitmap.createBitmap(backgroundBitmap.getWidth(),
                backgroundBitmap.getHeight(), backgroundBitmap.getConfig());

        // Scale the emoji so it looks better on the face
        float scaleFactor = EMOJI_SCALE_FACTOR;

        // Determine the size of the emoji to match the width of the face and preserve aspect ratio
        int newEmojiWidth = (int) (face.getWidth() * scaleFactor);
        int newEmojiHeight = (int) (emojiBitmap.getHeight() *
                newEmojiWidth / emojiBitmap.getWidth() * scaleFactor);


        // Scale the emoji
        emojiBitmap = Bitmap.createScaledBitmap(emojiBitmap, newEmojiWidth, newEmojiHeight, false);

        // Determine the emoji position so it best lines up with the face
        float emojiPositionX =
                (face.getPosition().x + face.getWidth() / 2) - emojiBitmap.getWidth() / 2;
        float emojiPositionY =
                (face.getPosition().y + face.getHeight() / 2) - emojiBitmap.getHeight() / 3;

        // Create the canvas and draw the bitmaps to it
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        canvas.drawBitmap(emojiBitmap, emojiPositionX, emojiPositionY, null);

        return resultBitmap;
    }
}
