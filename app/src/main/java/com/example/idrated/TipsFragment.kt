package com.example.idrated

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.idrated.databinding.FragmentTipsBinding

class TipsFragment : Fragment() {

    private var _binding: FragmentTipsBinding? = null
    private val binding get() = _binding!!

    private val tipsList = listOf(
        TipItem(
            R.drawable.ic_tp1,
            "Smart Hydration Tips for a Healthier You",
            "'Our bodies are roughly 60 percent water, so even the slightest bit of dehydration can negatively affect how we function, from energy levels to digestion and even immunity,' says Lisa Moskovitz, RDN, founder of the New York Nutrition Group in New York City. \n\n1. Hydrate When You Wake Up and Before Meals\n2. Wrap Up Your Day With Another Bottle of Water\n3. Eat Your Water by Following a Produce-Heavy Diet\n4. Experiment With How You Drink Water to Maximize Enjoyment\n5. Keep Track of Hydration With a Smartphone App.",
            "https://www.everydayhealth.com/dehydration/smart-tips-for-staying-hydrated-throughout-the-day/"
        ),
        TipItem(
            R.drawable.ic_tp2,
            "When to Pick Electrolyte Drinks Over Water",
            "For intense exercise or hot weather, water is usually enough. However, electrolyte drinks help replenish minerals lost through sweat.\n" +
                    "\n" +
                    "Recommendations:\n" +
                    "\n" +
                    "Hydrate early by drinking water before exercise.\n" +
                    "Anticipate hydration based on workout intensity and weather.\n" +
                    "Choose sports drinks with 4-8% carbs for high-intensity exercise.\n" +
                    "Pair water with salty snacks, like nuts, to replenish electrolytes.\n" +
                    "Try coconut or cactus water as a low-sugar alternative to sports drinks.\n" +
                    "Make your own electrolyte drink using celery, apple, and lemon.\n" +
                    "Exercise early or late to avoid peak heat.",
            "https://www.scripps.org/news_items/3988-when-to-pick-electrolyte-drinks-over-water"
        ),
        TipItem(
            R.drawable.ic_tp3,
            "Hydration Tips for Medically Special Cases",
            "Kidney Patients: Follow the water intake recommendations of your healthcare provider, as excess water can strain the kidneys.\n" +
                    "Heart Conditions: Monitor fluid intake to avoid overloading the heart. Use the app to track amounts and stay within prescribed limits.\n" +
                    "Diabetes: Stay hydrated to help manage blood sugar levels.\n" +
                    "Pregnancy and Breastfeeding: Drink more water to support increased fluid needs during these periods.\n" +
                    "Elderly Individuals: Remind users that thirst signals may weaken with age, so they should drink at regular intervals.",
            "https://www.healthline.com/health/kidney-health/kidney-failure-drinking-too-much-water"
        ),
        TipItem(
            R.drawable.ic_tp4,
            "Fueling and Hydrating Before, During, and After Exercise",
            "\nRecommendations:\n" +
                    "\n" +
                    "Before exercise: Eat a balanced meal with carbs, protein, and low fat 3-4 hours prior, and have a carb-rich snack with hydration 30-60 minutes before.\n" +
                    "During exercise: Drink water for sessions under 60 minutes, and opt for a snack and sports drink for sessions over 60 minutes.\n" +
                    "After exercise: Consume carbs and protein within 15-60 minutes, rehydrate with 16-24 oz. of water per pound lost, and have a balanced meal 2-3 hours after.",
            "https://www.nationwidechildrens.org/specialties/sports-medicine/sports-medicine-articles/fueling-and-hydrating-before-during-and-after-exercise"
        ),
        TipItem(
            R.drawable.ic_tp5,
            "Staying Hydrated with Water-Rich Foods",
            "To stay hydrated, boost your intake of water-rich foods like fruits and vegetables while still drinking enough fluids. Incorporating smoothies, soups, and raw veggies can help you meet hydration needs.\n\nRecommendations:\n" +
                    "\n" +
                    "Start your day with a smoothie (fruits + milk or water).\n" +
                    "Snack on raw veggies, grapes, or watermelon.\n" +
                    "Have a large salad with colorful veggies for lunch.\n" +
                    "Enjoy soup for dinner.\n" +
                    "Add water-rich foods like berries or cooked veggies to meals.\n" +
                    "Drink water with meals and make it more enjoyable by adding herbs, cucumber, or fruit.",
            "https://www.health.harvard.edu/staying-healthy/using-food-to-stay-hydrated"
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTipsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide the CardView (Option 1)
        binding.cardView.visibility = View.GONE

        // Set up ViewPager2 with the adapter
        val adapter = TipsPagerAdapter(tipsList) { url ->
            if (url.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }

        binding.viewPager.adapter = adapter
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class TipItem(val imageRes: Int, val title: String, val description: String, val link: String)
