package edu.thesis.fct.client.faceproc;

public class LinearRegression {
	
	//private final static double DBL_EPSILON = 0.00001;
	private static final double DBL_EPSILON = 1e-9;
	
	/*class Point2D
	{
		private double x, y;
	    
		public   Point2D() 
		{
			x = 0.0;
			y = 0.0;
		}

	    void setPoint(double X, double Y) { x = X; y = Y; }
	    void setX(double X) { x = X; }
	    void setY(double Y) { y = Y; }

	    double getX()  { return x; }
	    double getY()  { return y; }

	    
	};
	*/
	
	//public LinearRegression(Point2D *p = 0, long size = 0)

        // Constructor using arrays of x values and y values
    public LinearRegression() 
    {
    	  n = 0;             
    	  sumX =sumY = 0.0;  
    	  sumXsquared=   sumYsquared = sumXY = a = b =coefD = coefC = stdError = 0.0;
    	
	}
   


        void addPoint(double x , double y){
        	 	n++;
        	    sumX += x;
        	    sumY += y;
        	    sumXsquared += x * x;
        	    sumYsquared += y * y;
        	    sumXY += x * y;
        	    Calculate();
        }

        // Must have at least 3 points to calculate
        // standard error of estimate.  Do we have enough data?
        protected boolean haveData()  { return (n > 2 ? true : false); }
        protected long items()  { return n; }

        public double getA()  { return a; }
        public double getB()  { return b; }

        public double getCoefDeterm()   { return coefD; }
        public double getCoefCorrel()  { return coefC; }
        public double getStdErrorEst()  { return stdError; }
        public double estimateY(double x)  { return (a + b * x); }

    protected long n;             // number of data points input so far
    protected   double sumX, sumY;  // sums of x and y
    protected  double sumXsquared; // sum of x squares
    protected   double  sumYsquared; // sum y squares
    protected  double sumXY;       // sum of x*y

    protected   double a, b;        // coefficients of f(x) = a + b*x
    protected    double coefD   ;    // coefficient of determination
    protected     double    coefC;       // coefficient of correlation
    protected     double    stdError;    // standard error of estimate

    protected  void Calculate()
    {
    	 if (haveData())
    	    {
    	        if (Math.abs( (double)n * sumXsquared - sumX * sumX) > DBL_EPSILON)
    	        {
    	            b = ( (double)n * sumXY - sumY * sumX) /
    	                ( (double)n * sumXsquared - sumX * sumX);
    	            a = (sumY - b * sumX) / (double)n;

    	            double sx = b * ( sumXY - sumX * sumY / (double)n );
    	            double sy2 = sumYsquared - sumY * sumY /(double)n;
    	            double sy = sy2 - sx;

    	            coefD = sx / sy2;
    	            coefC = Math.sqrt(coefD);
    	            stdError = Math.sqrt(sy / (double)(n - 2));
    	        }
    	        else
    	        {
    	            a = b = coefD = coefC = stdError = 0.0;
    	        }
    	    }
    }

}
