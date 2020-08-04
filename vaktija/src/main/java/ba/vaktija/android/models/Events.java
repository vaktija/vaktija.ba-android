package ba.vaktija.android.models;

public class Events {

    public static class PrayerSilentSettingsChanged {
    }

    public static class RingerModeChanged {
    }

    public static class SkipSilentEvent {
        int prayerId;

        public SkipSilentEvent(int prayerId) {
            this.prayerId = prayerId;
        }

        public int getPrayerId() {
            return prayerId;
        }

        public void setPrayerId(int prayerId) {
            this.prayerId = prayerId;
        }
    }

    public static class PrayerChangedEvent {
    }

    public static class PrayerUpdatedEvent {
        int prayerId;

        public PrayerUpdatedEvent(int prayerId) {
            this.prayerId = prayerId;
        }

        public int getPrayerId() {
            return prayerId;
        }

        public void setPrayerId(int prayerId) {
            this.prayerId = prayerId;
        }
    }
}
