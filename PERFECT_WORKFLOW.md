# 🎯 PERFECT WORKFLOW - Nutrition Application

## ✅ **FIXES APPLIED**
1. **Removed 2-goal limit** - Now unlimited goals
2. **Limited suggestions to 10** - Changed from 20 to 10
3. **Fixed chart data persistence** - Proper cleanup when meals deleted
4. **Enhanced meal deletion** - Cascading deletes for all related data
5. **Fixed goal targeting** - Now targets the actual goal amount instead of finding highest nutrient foods
6. **Improved validation** - Prevents tiny goal amounts (< 1g)

## 🚀 **PERFECT TESTING WORKFLOW**

### **Step 1: Start Application**
```bash
mvn exec:java -Dexec.mainClass="ca.yorku.eecs3311.nutrisci.Main"
```

### **Step 2: Database Setup (First Time Only)**
- Application automatically initializes database
- Creates all tables and populates with nutrition data
- Fixes any invalid meal items automatically

### **Step 3: Add Multiple Meals (Log Meal Tab)**

#### **Meal 1: High Protein Breakfast**
- **Date**: Today's date
- **Meal Type**: BREAKFAST
- **Food**: "Chicken, broilers or fryers, breast, meat only, cooked, roasted"
- **Quantity**: 100
- **Unit**: g
- **Click**: "Save Records"

#### **Meal 2: High Carb Lunch**
- **Date**: Today's date
- **Meal Type**: LUNCH
- **Food**: "Rice, white, long-grain, regular, enriched, cooked"
- **Quantity**: 150
- **Unit**: g
- **Click**: "Save Records"

#### **Meal 3: High Fat Snack**
- **Date**: Today's date
- **Meal Type**: SNACK
- **Food**: "Nuts, almonds"
- **Quantity**: 50
- **Unit**: g
- **Click**: "Save Records"

### **Step 4: Verify Daily Summary (Visualization Tab)**
- **Check**: Daily Nutrition Chart shows all 3 meals combined
- **Verify**: Calories, Protein, Carbs, Fats are calculated correctly
- **Note**: Should show meaningful values (not 0.0)

### **Step 5: Create Multiple Swap Goals (Swap Tab)**

#### **Goal 1: Increase Protein**
- **Nutrient**: PROTEIN
- **Direction**: INCREASE
- **Amount**: 5
- **Unit**: g
- **Click**: "Add Goal"
- **Expected**: System will find foods that provide ~5g more protein

#### **Goal 2: Decrease Carbs**
- **Nutrient**: CARBOHYDRATE, TOTAL (DIETARY FIBER)
- **Direction**: DECREASE
- **Amount**: 10
- **Unit**: g
- **Click**: "Add Goal"
- **Expected**: System will find foods with ~10g less carbs

#### **Goal 3: Increase Fats**
- **Nutrient**: TOTAL LIPID (FAT)
- **Direction**: INCREASE
- **Amount**: 3
- **Unit**: g
- **Click**: "Add Goal"
- **Expected**: System will find foods that provide ~3g more fat

### **Step 6: Generate Swap Suggestions**
- **Click**: "Generate Suggestions"
- **Verify**: Shows exactly 10 suggestions (not 20)
- **Check**: Suggestions target the goal amounts (not highest nutrient foods)
- **Note**: Should show foods that provide the requested increase/decrease

### **Step 7: Apply a Swap**
- **Select**: One suggestion from the list
- **Click**: "Apply Swap"
- **Verify**: Success message appears
- **Check**: Meal is updated in the database

### **Step 8: Verify Swap Results**
- **Go to**: Visualization Tab
- **Check**: Comparison Chart shows "Before" and "After" values
- **Verify**: Values are different (not the same)
- **Note**: Should show the actual nutrient changes matching the goal

### **Step 9: Test Meal Deletion**
- **Go to**: Log Meal Tab
- **Select**: One meal from "Saved Meals"
- **Click**: "Delete Meal"
- **Verify**: Meal is removed from list
- **Check**: Charts update accordingly

### **Step 10: Test Complete Data Cleanup**
- **Click**: "Delete All Meals"
- **Confirm**: Yes to the warning dialog
- **Verify**: All meals are deleted
- **Check**: Charts show "No data available"
- **Note**: Daily summaries are also cleaned up

### **Step 11: Re-add Meals and Test Again**
- **Add**: New meals following Step 3
- **Create**: New swap goals following Step 5
- **Generate**: New suggestions
- **Apply**: New swaps
- **Verify**: Everything works correctly

## 🔧 **TECHNICAL VERIFICATIONS**

### **Database Checks**
```sql
-- Check meals
SELECT COUNT(*) FROM meals WHERE user_id = 8;

-- Check daily summaries
SELECT COUNT(*) FROM daily_summary WHERE user_id = 8;

-- Check recommendations
SELECT COUNT(*) FROM recommendations r 
JOIN swap_goals sg ON r.goal_id = sg.id 
WHERE sg.user_id = 8;

-- Check swap goals
SELECT COUNT(*) FROM swap_goals WHERE user_id = 8;
```

### **Expected Behaviors**
1. **Goals**: Unlimited number allowed
2. **Suggestions**: Maximum 10 shown
3. **Charts**: Accurate before/after comparison
4. **Deletion**: Complete cleanup of all related data
5. **Recalculation**: Automatic after swaps
6. **Multi-meal**: Support for complex swap combinations

## 🎯 **SUCCESS CRITERIA**

✅ **Unlimited Goals**: Can add more than 2 goals
✅ **10 Suggestions**: Exactly 10 swap suggestions shown
✅ **Accurate Charts**: Before/after values are different
✅ **Complete Cleanup**: No orphaned data after deletion
✅ **Multi-meal Swaps**: Complex combinations work
✅ **Real-time Updates**: Charts update immediately after changes
✅ **Data Integrity**: No foreign key violations
✅ **User Experience**: Clear feedback and confirmations

## 🚨 **TROUBLESHOOTING**

### **If Charts Show Same Values**
- Check that `recommendations` table has `original_food_id` column
- Verify `storeRecommendation` is called BEFORE meal update
- Ensure proper transaction handling

### **If Suggestions Don't Appear**
- Check that meals exist for the user
- Verify nutrient data in database
- Check conversion factors exist for suggested foods

### **If Deletion Fails**
- Check foreign key constraints
- Verify transaction rollback on errors
- Ensure proper deletion order

### **If Goals Don't Save**
- Check `swap_goals` table exists
- Verify user ID is correct
- Check for SQL exceptions in logs

## 🎉 **PERFECT WORKFLOW COMPLETE**

This workflow tests all major features:
- ✅ Meal logging and nutrition calculation
- ✅ Multiple swap goals (unlimited)
- ✅ Smart swap suggestions (10 max)
- ✅ Multi-meal swap combinations
- ✅ Accurate comparison charts
- ✅ Complete data cleanup
- ✅ Real-time updates
- ✅ Data integrity

The application should now work perfectly with no hardcoded limits and proper data handling! 