# Tick Sound Setup

The countdown timer now includes a tick sound that plays every second. This file explains how to set up the sound.

## Sound File Location
The tick sound should be placed in: `app/src/main/res/raw/tick.mp3`

## Current Status
- `tick.mp3` - This is currently a placeholder file that needs to be replaced

## How to Add Your Tick Sound

1. **Replace the placeholder**: Delete the current `tick.mp3` file in `app/src/main/res/raw/` and replace it with your actual tick sound file.

2. **Sound Requirements**:
   - Format: MP3
   - Duration: Less than 500ms (to prevent overlap)
   - Volume: Moderate (not too loud)
   - Type: Short "tick", "beep", or "click" sound

3. **Recommended Sources**:
   - Free sound effect websites (freesound.org, zapsplat.com)
   - Create your own using audio editing software
   - Use system notification sounds (ensure you have rights)

4. **Testing**: After adding the sound file, test the countdown timer to ensure:
   - The sound plays every second
   - No overlapping sounds
   - Appropriate volume level

## Implementation Details

The tick sound is implemented in `RequestTowFragment.kt`:
- `initializeTickSound()` - Sets up the MediaPlayer
- `playTickSound()` - Plays the sound each second
- `releaseTickSound()` - Cleans up resources

The sound plays every second during the 2-minute countdown timer.

## Build Error Fix
The build error occurred because documentation files should not be placed in resource folders. The README has been moved to the project root. 