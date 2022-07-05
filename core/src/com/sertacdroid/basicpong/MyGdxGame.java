package com.sertacdroid.basicpong;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Align;

import java.util.*;

public class MyGdxGame implements ApplicationListener, InputProcessor
{
	private enum Direction
	{
		UP,
		RIGHT,
		DOWN,
		LEFT
	}

	private static class Collision
	{
		public Boolean detected;
		public Direction direction;
		public Vector2 difference;
		public Boolean stickyPaddle;

		public Collision(Boolean a, Direction b, Vector2 c, Boolean d)
		{
			this.detected = a;
			this.direction = b;
			this.difference = c;
			this.stickyPaddle = d;
		}
	}

	private static class TouchInfo
	{
		public Float x;
		public Float y;
	}

	private final Map<Integer, TouchInfo> playerTouch = new HashMap<>();

	private static class Rctngl
	{
		private final Texture texture;
		private final Sprite sprite;
		private final int x;
		private final int y;
		private final int width;
		private final int height;

		public Rctngl(int a, int b, int c, int d, boolean fill, int frameWidth, Color color)
		{
			this.x = a;
			this.y = b;
			this.width = c;
			this.height = d;

			Pixmap pixmap = new Pixmap(this.width, this.height, Pixmap.Format.RGBA8888);
			pixmap.setColor(color);

			if(fill)
			{
				pixmap.fillRectangle(0, 0, pixmap.getWidth(), pixmap.getHeight());
			}
			else
			{
				for(int i = 0; i < frameWidth; i++)
				{
					pixmap.drawRectangle(i, i, pixmap.getWidth() - 2*i, pixmap.getHeight() - 2*i);
				}
			}

			this.texture = new Texture(pixmap);
			pixmap.dispose();
			this.sprite = new Sprite(this.texture);
			this.sprite.setPosition(this.x, this.y);
		}
	}

	private static class BaseLine
	{
		private final Texture texture;
		private final Sprite sprite;
		private final int x;

		public BaseLine(int a, int b, int c, int d)
		{
			this.x = a;
			Pixmap pixmap = new Pixmap(b, Gdx.graphics.getHeight(), Pixmap.Format.RGBA8888);
			pixmap.setColor(Color.WHITE);

			if (c == 0)
			{
				pixmap.fillRectangle(0, 0, pixmap.getWidth(), pixmap.getHeight());
			}
			else
			{
				if (d > 0)
				{
					int y = 0;

					while(y < pixmap.getHeight())
					{
						int spanL = d;

						if (y + spanL > pixmap.getHeight()) spanL = pixmap.getHeight() - y;

						pixmap.fillRectangle(0, y, pixmap.getWidth(), spanL);

						y += spanL + c;
					}
				}
				else
				{
					pixmap.fillRectangle(0, 0, pixmap.getWidth(), pixmap.getHeight());
				}
			}

			this.texture = new Texture(pixmap);
			pixmap.dispose();
			this.sprite = new Sprite(this.texture);
			this.sprite.setPosition(this.x, 0);
		}
	}

	private static class Ball
	{
		private final Texture texture;
		private final Sprite sprite;
		private final Vector2 position;
		private final Vector2 direction;
		private final Float radius;
		private final Float startSpeed;
		private Float speed;
		private Float minSpeed;
		private final Float maxSpeed;
		private int trappedCollision;

		public Ball(float px, float py, float dx, float dy, float r, float n, float x)
		{
			this.position = new Vector2(px, py);
			this.direction = new Vector2(dx, dy);
			this.radius = r;
			this.minSpeed = n;
			this.maxSpeed = x;
			this.startSpeed = n;
			this.speed = n;
			this.trappedCollision = 0;
			this.direction.nor();
			Pixmap pixmap = new Pixmap(this.radius.intValue() * 2 + 1, this.radius.intValue() * 2 + 1, Pixmap.Format.RGBA8888);
			pixmap.setColor(Color.WHITE);
			pixmap.fillCircle(pixmap.getWidth()/2, pixmap.getHeight()/2, this.radius.intValue());
			this.texture = new Texture(pixmap);
			pixmap.dispose();
			this.sprite = new Sprite(this.texture);
			this.sprite.setPosition(this.position.x, this.position.y);
		}
	}

	private static class Racket
	{
		private final Texture texture;
		private final Sprite sprite;
		private final Vector2 position;
		private final Vector2 size;
		private final Vector2 direction;
		private Float speed;
		private Float curSpeed;
		private final Float minSpeed;
		private final Float maxSpeed;
		private float yCenter;

		public Racket(float sx, float sy, float px, float py, float dx, float dy, float n, float x)
		{
			this.size = new Vector2(sx, sy);
			this.position = new Vector2(px, py);
			this.direction = new Vector2(dx, dy);
			this.direction.nor();
			this.minSpeed = n;
			this.maxSpeed = x;
			this.curSpeed = n;
			this.speed = this.curSpeed;
			this.yCenter = this.size.y/2;
			Pixmap pixmap = new Pixmap(((Float) this.size.x).intValue(), ((Float) this.size.y).intValue(), Pixmap.Format.RGBA8888);
			pixmap.setColor(Color.WHITE);
			pixmap.fillRectangle(0, 0, ((Float) this.size.x).intValue(), ((Float) this.size.y).intValue());
			this.texture = new Texture(pixmap);
			pixmap.dispose();
			this.sprite = new Sprite(this.texture);
			this.sprite.setPosition(this.position.x, this.position.y);
		}
	}

	private static class MenuItem
	{
		private final Vector2 position;
		private final Vector2 size;
		String[] text;
		int textIndex;
		boolean clicked;

		public MenuItem(Vector2 a, Vector2 b, String[] c, int d, boolean e)
		{
			this.position = a;
			this.size = b;
			this.text = c;
			this.textIndex = d;
			this.clicked = e;
		}
	}

	private class GamePlay
	{
		private boolean soundOn;
		private boolean twoPlayer;
		private boolean paused;
		private boolean newGame;
		private boolean firstPass;
		private int firstTouched;
		private int playerOneScore;
		private int playerTwoScore;
		private boolean goalOne;
		private boolean goalTwo;
		private final BitmapFont scoreFont;
		private String currentWindow;

		private final Rctngl pauseMenuBackground;
		private final Rctngl pauseMenuFrame;
		private final BitmapFont pauseMenuFontUp;
		private final BitmapFont pauseMenuFontDown;

		private final Rctngl aboutBackground;
		private final Rctngl aboutFrame;
		private final BitmapFont aboutFont;
		private final String aboutText;
		private final float aboutTextX;
		private final float aboutTextY;
		private float aboutTextAlignWidth;
		private float aboutTextAlignHeight;

		GlyphLayout layout = new GlyphLayout();

		private final String startText = "Touch to Start Game";

		private final MenuItem[] pauseMenuItem;

		GamePlay(int fontSize)
		{
			this.soundOn = prefs.getBoolean("soundOn", false);
			this.twoPlayer = prefs.getBoolean("twoPlayer", false);
			this.paused = true;
			this.newGame = true;
			this.firstPass = true;
			this.playerOneScore = 0;
			this.playerTwoScore = 0;
			this.goalOne = false;
			this.goalTwo = false;

			String[][] menuItem = {
					{"BACK"},
					{"NEW GAME"},
					{"ONE PLAYER", "TWO PLAYER"},
					{"SOUND OFF", "SOUND ON"},
					{"ABOUT"},
					{"EXIT"}
			};
			if (this.twoPlayer)
			{
				menuItem[2][0] = "TWO PLAYER";
				menuItem[2][1] = "ONE PLAYER";
			}

			if (this.soundOn)
			{
				menuItem[3][0] = "SOUND ON";
				menuItem[3][1] = "SOUND OFF";
			}

			this.currentWindow = "MainMenu";

			this.scoreFont = new BitmapFont();
			this.scoreFont.setColor(Color.WHITE);
			this.scoreFont.getData().setScale(fontSize);

			this.pauseMenuBackground = new Rctngl(
					Gdx.graphics.getWidth() * 3 / 8,
					Gdx.graphics.getHeight() / 4,
					Gdx.graphics.getWidth() / 4,
					Gdx.graphics.getHeight() / 2,
					true,
					0,
					Color.DARK_GRAY
			);

			this.pauseMenuFrame = new Rctngl(
					this.pauseMenuBackground.x,
					this.pauseMenuBackground.y,
					this.pauseMenuBackground.width,
					this.pauseMenuBackground.height,
					false,
					5,
					Color.GRAY
			);

			this.pauseMenuFontUp = new BitmapFont();
			this.pauseMenuFontUp.setColor(Color.YELLOW);

			this.pauseMenuFontDown = new BitmapFont();
			this.pauseMenuFontDown.setColor(Color.WHITE);

			int pauseMenuFontScale = 5;
			this.pauseMenuFontDown.getData().setScale(pauseMenuFontScale);
			this.pauseMenuFontUp.getData().setScale(pauseMenuFontScale);

			int maxMenuItemLength = 0;
			int[] menuItemIndexWithMaxLength = {0,0};

			for(int i = 0; i< menuItem.length; i++)
			{
				for(int j = 0; j< menuItem[i].length; j++)
				{
					if(menuItem[i][j].length() > maxMenuItemLength)
					{
						maxMenuItemLength = menuItem[i][j].length();
						menuItemIndexWithMaxLength[0] = i;
						menuItemIndexWithMaxLength[1] = j;
					}
				}
			}

			while(true)
			{
				this.layout.setText
				(
					pauseMenuFontDown,
						menuItem[menuItemIndexWithMaxLength[0]]
										[menuItemIndexWithMaxLength[1]]
				);
				float a = this.layout.width;
				this.layout.setText
				(
						pauseMenuFontDown,
						"OOOO"
				);
				float b = this.layout.width;
				if(a <= this.pauseMenuBackground.width - b) break;
				this.pauseMenuFontDown.getData().setScale(--pauseMenuFontScale);
			}

			int maxMenuItemHeight = 0;
			int[] menuItemIndexWithMaxHeight = {0,0};

			for(int i = 0; i< menuItem.length; i++)
			{
				for (int j = 0; j < menuItem[i].length; j++)
				{
					this.layout.setText
					(
							pauseMenuFontDown,
							menuItem[i][j]
					);
					float a = this.layout.height;
					if (a > maxMenuItemHeight)
					{
						maxMenuItemHeight = ((int) a);
						menuItemIndexWithMaxHeight[0] = i;
						menuItemIndexWithMaxHeight[1] = j;
					}
				}
			}

			int blankRowHeight = maxMenuItemHeight;

			while(true)
			{
				this.layout.setText
				(
					pauseMenuFontDown,
						menuItem[menuItemIndexWithMaxHeight[0]]
										[menuItemIndexWithMaxHeight[1]]
				);
				float a = this.layout.height;

				if(a <= this.pauseMenuBackground.height - ( ( ( a + blankRowHeight) * menuItem.length ) + blankRowHeight)) break;
				this.pauseMenuFontDown.getData().setScale(--pauseMenuFontScale);

				maxMenuItemHeight = ((int) a);

				blankRowHeight = maxMenuItemHeight;
			}

			this.pauseMenuFontUp.getData().setScale(pauseMenuFontScale);

			this.layout.setText
			(
				pauseMenuFontDown,
					menuItem[menuItemIndexWithMaxHeight[0]]
									[menuItemIndexWithMaxHeight[1]]
			);
			float a = this.layout.height;

			int verticalBlankHeight = (this.pauseMenuBackground.height - ((int) ((a + blankRowHeight) * menuItem.length - blankRowHeight))) / 2;

			this.pauseMenuItem = new MenuItem[menuItem.length];

			int nextRowY = this.pauseMenuBackground.y + this.pauseMenuBackground.height - verticalBlankHeight;

			for(int i=0; i<this.pauseMenuItem.length; i++)
			{
				this.layout.setText
				(
					pauseMenuFontDown,
					menuItem[i][0]
				);
				float b = this.layout.width;
				float c = this.layout.height;

				this.pauseMenuItem[i] = new MenuItem(
						new Vector2(this.pauseMenuBackground.x + (this.pauseMenuBackground.width - b) / 2, nextRowY - c),
						new Vector2(b, c),
						menuItem[i],
						0,
						false
				);

				nextRowY -= c + blankRowHeight;
			}

			this.aboutBackground = new Rctngl(
					Gdx.graphics.getWidth() * 2 / 5,
					Gdx.graphics.getHeight() / 3,
					Gdx.graphics.getWidth() / 5,
					Gdx.graphics.getHeight() / 3,
					true,
					0,
					Color.DARK_GRAY
			);

			this.aboutFrame = new Rctngl(
					this.aboutBackground.x,
					this.aboutBackground.y,
					this.aboutBackground.width,
					this.aboutBackground.height,
					false,
					5,
					Color.GRAY
			);

			this.aboutFont = new BitmapFont();
			this.aboutFont.setColor(Color.WHITE);

			float aboutFontScale = 4;
			this.aboutFont.getData().setScale(aboutFontScale);

			this.aboutText = "Basic Pong\nCoded By Sertaç\n2016";

			this.layout.setText
			(
				aboutFont,
				this.aboutText
			);

			this.aboutTextAlignWidth = this.layout.width;
			this.aboutTextAlignHeight = this.layout.height;

			if (this.aboutTextAlignHeight > this.aboutBackground.height * 0.9)
			{
				aboutFontScale *= 1 - ((this.aboutTextAlignHeight - this.aboutBackground.height * 0.9) / this.aboutTextAlignHeight);
				this.aboutFont.getData().setScale(aboutFontScale);
			}

			if (this.aboutTextAlignWidth > this.aboutBackground.width * 0.9)
			{
				aboutFontScale *= 1 - ((this.aboutTextAlignWidth - this.aboutBackground.width * 0.9) / this.aboutTextAlignWidth);
				this.aboutFont.getData().setScale(aboutFontScale);
			}

			this.layout.setText
					(
							aboutFont,
							this.aboutText
					);

			this.aboutTextAlignWidth = this.layout.width;
			this.aboutTextAlignHeight = this.layout.height;

			this.aboutTextX = this.aboutBackground.x + (this.aboutBackground.width - this.aboutTextAlignWidth)/2;
			this.aboutTextY = this.aboutBackground.y + this.aboutBackground.height - (this.aboutBackground.height - this.aboutTextAlignHeight)/2;
		}

		public void drawPauseMenu()
		{
			this.pauseMenuBackground.sprite.draw(batch);
			this.pauseMenuFrame.sprite.draw(batch);

			for (MenuItem menuItem : this.pauseMenuItem) {
				if (!menuItem.clicked) {
					this.pauseMenuFontDown.draw
							(
									batch,
									menuItem.text[menuItem.textIndex],
									menuItem.position.x,
									menuItem.position.y + menuItem.size.y
							);
				} else {
					this.pauseMenuFontUp.draw
							(
									batch,
									menuItem.text[menuItem.textIndex],
									menuItem.position.x,
									menuItem.position.y + menuItem.size.y
							);
				}
			}
		}

		public void drawAboutWindow()
		{
			this.aboutBackground.sprite.draw(batch);
			this.aboutFrame.sprite.draw(batch);

			this.layout.setText
					(
							aboutFont,
							this.aboutText
					);
			float a = this.layout.height;

			this.layout.setText
					(
							aboutFont,
							this.aboutText,
							this.scoreFont.getColor(),
							this.aboutTextAlignWidth,
							Align.center,
							true
					);

			this.aboutFont.draw
					(
							batch,
							this.layout,
							this.aboutTextX,
							this.aboutTextY + this.aboutTextAlignHeight - a
					);
		}

		public int checkGoal(Ball one, Racket[] two)
		{
			int goaler = -1;

			if(one.position.x + one.radius < two[0].position.x)
			{
				if(!this.goalTwo)
				{
					this.playerTwoScore += 1;
					goaler = 1;
				}

				this.goalTwo = true;
			}
			else
			{
				this.goalTwo = false;
			}

			if(one.position.x > two[1].position.x + two[1].size.x)
			{
				if(!this.goalOne)
				{
					this.playerOneScore += 1;
					goaler = 0;
				}

				this.goalOne = true;
			}
			else
			{
				this.goalOne = false;
			}

			return goaler;
		}
	}

	private Preferences prefs;
	private SpriteBatch batch;
	//private BitmapFont font;
	private GamePlay game;
	private Rctngl frame;
	private BaseLine line;
	private Ball gameBall;
	private Racket[] playerRacket;
	private Sound[] racketSound;
	private Sound wallSound;

	private void boundaryCollisions(Ball one, Racket[] two)
	{
		boolean wallCollision = false;

		if (one.position.x < 0.0f)
		{
			one.position.x = 0.0f;
			one.direction.x = -one.direction.x;

			wallCollision = true;
		}

		if (one.position.x + one.radius*2.0f > Gdx.graphics.getWidth())
		{
			one.position.x = Gdx.graphics.getWidth() - one.radius*2.0f;
			one.direction.x = -one.direction.x;

			wallCollision = true;
		}

		if (one.position.y < 0.0f)
		{
			one.position.y = 0.0f;
			one.direction.y = -one.direction.y;

			wallCollision = true;
		}

		if (one.position.y + one.radius*2.0f > Gdx.graphics.getHeight())
		{
			one.position.y = Gdx.graphics.getHeight() - one.radius*2.0f;
			one.direction.y = -one.direction.y;

			wallCollision = true;
		}

		if (wallCollision)
		{
			if(game.soundOn) wallSound.play();
		}

		for (int i=0; i<2; i++)
		{
			if (two[i].position.y < 0.0f) two[i].position.y = 0.0f;
			if (two[i].position.y > Gdx.graphics.getHeight() - two[i].size.y) two[i].position.y = Gdx.graphics.getHeight() - two[i].size.y;
		}

		if
		(
				(
						one.position.x + one.radius < (float) Gdx.graphics.getWidth()/2 &&
								one.position.x + one.radius < two[0].position.x &&
								one.position.y + one.radius <= two[0].position.y + two[0].size.y &&
								one.position.y + one.radius > two[0].position.y
				)
						||
						(
								one.position.x + one.radius >= (float) Gdx.graphics.getWidth()/2 &&
										one.position.x + one.radius > two[1].position.x &&
										one.position.y + one.radius <= two[1].position.y + two[1].size.y &&
										one.position.y + one.radius > two[1].position.y
						)
		)
		{
			one.trappedCollision++;
		}
		else
		{
			one.trappedCollision = 0;
		}
	}

	private Direction vectorDirection(Vector2 Target)
	{
		Vector2[] compass = new Vector2[]
				{
						new Vector2(0.0f, 1.0f),
						new Vector2(1.0f, 0.0f),
						new Vector2(0.0f, -1.0f),
						new Vector2(-1.0f, 0.0f),
				};

		float max = 0.0f;
		Direction bestDirection = Direction.LEFT;

		for (int i = 0; i < 4; i++)
		{
			float dotProduct = Target.nor().dot(compass[i]);

			if (dotProduct > max)
			{
				max = dotProduct;

				switch(i)
				{
					case 0: bestDirection = Direction.UP; break;
					case 1: bestDirection = Direction.RIGHT; break;
					case 2: bestDirection = Direction.DOWN; break;
					case 3: bestDirection = Direction.LEFT; break;
				}
			}
		}

		return bestDirection;
	}

	private Collision checkCollision(Ball one, Racket two)
	{
		Vector2 center = (new Vector2(one.position)).add(new Vector2(one.radius, one.radius));
		Vector2 aabb_half_extents = new Vector2(two.size.x/2, two.size.y/2);
		Vector2 aabb_center = (new Vector2(two.position)).add(aabb_half_extents);
		Vector2 difference = (new Vector2(center)).sub(aabb_center);
		Vector2 clamped = new Vector2();
		clamped.x = MathUtils.clamp(difference.x, -aabb_half_extents.x, aabb_half_extents.x);
		clamped.y = MathUtils.clamp(difference.y, -aabb_half_extents.y, aabb_half_extents.y);
		Vector2 closest = (new Vector2(aabb_center)).add(clamped);
		Vector2 difference2 = (new Vector2(closest)).sub(center);

		boolean stickyPaddle = center.x >= two.position.x &&
				center.x <= two.position.x + two.size.x &&
				center.y >= two.position.y &&
				center.y <= two.position.y + two.size.y;

		if (difference2.len() < one.radius)
		{
			if
			(
					(
							one.position.y + one.radius <= two.position.y + two.size.y &&
									one.position.y + one.radius > two.position.y
					)
							&&
							(
									(
											one.position.x + one.radius < (float) Gdx.graphics.getWidth()/2 &&
													one.position.x + one.radius < two.position.x
									)
											||
											(
													one.position.x + one.radius >= (float) Gdx.graphics.getWidth()/2 &&
															one.position.x + one.radius > two.position.x
											)
							)
			)
			{
				one.trappedCollision++;
			}
			else
			{
				one.trappedCollision = 0;
			}

			return (new Collision(true, vectorDirection(difference2), difference2, stickyPaddle));
		}
		else
		{
			return (new Collision(false, Direction.UP, (new Vector2(0.0f, 0.0f)), stickyPaddle));
		}
	}

	private void collisionReposition(Collision c, Ball one)
	{
		if (c.direction == Direction.LEFT || c.direction == Direction.RIGHT)
		{
			one.direction.x = -one.direction.x;

			float penetration = one.radius - c.difference.x;

			if (c.direction == Direction.LEFT)
			{
				one.position.x += penetration;
			}
			else
			{
				one.position.x -= penetration;
			}
		}
		else
		{
			one.direction.y = -one.direction.y;

			float penetration = one.radius - c.difference.y;

			if (c.direction == Direction.UP)
			{
				one.position.y -= penetration;
			}
			else
			{
				one.position.y += penetration;
			}
		}
	}

	private void collisionRedirection(Collision c, Ball one, Racket two)
	{
		float centerboard = two.position.y + two.size.y / 2.0f;
		float distance = (one.position.y + one.radius) - centerboard;
		float percentage = 2.0f * distance / two.size.y;

		if (c.direction == Direction.LEFT || c.direction == Direction.RIGHT)
		{

			if (distance > 0.0f)
			{
				if (one.direction.y < 0.0f)
				{
					one.direction.y = -one.direction.y;

					float penetrationX = one.radius - c.difference.x;

					one.position.y += Math.abs((penetrationX / one.direction.x) * one.direction.y);
				}
			}

			if (distance < 0.0f)
			{
				if (one.direction.y > 0.0f)
				{
					one.direction.y = -one.direction.y;

					float penetrationX = one.radius - c.difference.x;

					one.position.y -= Math.abs((penetrationX / one.direction.x) * one.direction.y);
				}
			}
		}

		if(c.direction == Direction.LEFT) {
			if (one.position.x >= two.position.x + two.size.x)
				one.direction.x = 1.0f;
			else
				one.direction.x = -1.0f;
		}
		else {
			if (one.position.x + one.radius <= two.position.x)
				one.direction.x = -1.0f;
			else
				one.direction.x = 1.0f;
		}

		one.direction.y = percentage;
		one.direction.nor();
	}

	private int computerRacketMove(Ball one, Racket two)
	{
		float oneCenter;
		float twoCenter;

		if (one.trappedCollision > 5)
		{
			twoCenter = two.position.y + two.yCenter;

			if ((float) Gdx.graphics.getWidth() / 2 < twoCenter)
			{
				two.direction.y = -1.0f;
			}
			else
			{
				two.direction.y = 1.0f;
			}
			two.direction.nor();
		}
		else
		{
			if
			(
					Math.abs(one.position.x - two.position.x) < (float) Gdx.graphics.getWidth() / 2
							&&
							(
									(one.direction.x > 0 && one.position.x + one.radius < two.position.x) ||
											(one.direction.x < 0 && one.position.x > two.position.x + two.size.x)
							)
			)
			{
				oneCenter = one.position.y + one.radius;
				twoCenter = two.position.y + two.yCenter;

				if (oneCenter < twoCenter)
				{
					two.direction.y = -1.0f;
					two.direction.nor();
				}
				else
				if (oneCenter > twoCenter)
				{
					two.direction.y = 1.0f;
					two.direction.nor();
				}
				else
				{
					two.direction.y = 0.0f;
					two.direction.nor();
				}

				two.speed = Math.min(Math.abs(oneCenter - twoCenter), two.curSpeed);
			}
			else
			{
				two.direction.y = 0.0f;
				two.direction.nor();

				two.speed = two.curSpeed;
			}
		}

		two.position.y += two.direction.y*two.speed;

		return (int) two.position.y;
	}

	@Override
	public void create()
	{
		prefs = Gdx.app.getPreferences("GameConfig");

		batch = new SpriteBatch();

		float ballRadius =(float)  Gdx.graphics.getHeight()/47;
		float racketSizeX = (float) Gdx.graphics.getWidth()/27;
		float racketSizeY = (float) Gdx.graphics.getWidth()/10;
		float ballMinSpeed = (float) Gdx.graphics.getWidth()/80;
		float ballMaxSpeed = (racketSizeX-1)/2;
		float racketMinSpeed = (float) Gdx.graphics.getHeight()/116;
		float racketMaxSpeed = (float) Gdx.graphics.getHeight()/72;
		int fontSize = Gdx.graphics.getWidth()/256;

		game = new GamePlay(fontSize);

		gameBall = new Ball(
				Gdx.graphics.getWidth() / 2.0f - ballRadius,
				Gdx.graphics.getHeight() / 2.0f - ballRadius + (((new Random()).nextInt(2) == 0) ? -5.0f : 5.0f),
				((new Random()).nextInt(2) == 0) ? -1.0f : 1.0f,
				0.0f,
				ballRadius,
				ballMinSpeed,
				ballMaxSpeed
		);

		playerRacket = new Racket[2];

		playerRacket[0] = new Racket(
				racketSizeX,
				racketSizeY,
				Gdx.graphics.getWidth() / 8.0f + racketSizeX / 2.0f,
				(Gdx.graphics.getHeight() + racketSizeY) / 2.0f,
				0.0f,
				0.0f,
				racketMinSpeed,
				racketMaxSpeed
		);

		playerRacket[1] = new Racket(
				racketSizeX,
				racketSizeY,
				Gdx.graphics.getWidth() - playerRacket[0].position.x - racketSizeX,
				playerRacket[0].position.y,
				0.0f,
				0.0f,
				racketMinSpeed,
				racketMaxSpeed
		);

		line = new BaseLine(
				Gdx.graphics.getWidth() / 2 - 2,
				4,
				8,
				32
		);

		frame = new Rctngl(
				0,
				0,
				Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight(),
				false,
				10,
				Color.WHITE
		);

		Gdx.input.setInputProcessor(this);
		Gdx.input.setCatchKey(Input.Keys.BACK, true);

		playerTouch.put(0, new TouchInfo());
		playerTouch.get(0).x = playerRacket[0].position.x;
		playerTouch.get(0).y = Gdx.graphics.getHeight()/2.0f;

		playerTouch.put(1, new TouchInfo());
		playerTouch.get(1).x = playerRacket[1].position.x;
		playerTouch.get(1).y = Gdx.graphics.getHeight()/2.0f;

		racketSound = new Sound[2];
		racketSound[0] = Gdx.audio.newSound(Gdx.files.internal("pongblipg5.wav"));
		racketSound[1] = Gdx.audio.newSound(Gdx.files.internal("pongblipg4.wav"));
		wallSound = Gdx.audio.newSound(Gdx.files.internal("pongblipg3.wav"));
	}

	@Override
	public void render()
	{
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		if(!game.newGame)
		{
			if(Gdx.input.isKeyJustPressed(Input.Keys.BACK))
			{
				if(Objects.equals(game.currentWindow, "MainMenu"))
				{
					game.paused = !game.paused;
				}
				else
				{
					game.currentWindow = "MainMenu";
				}
			}
		}

		if(!game.paused || game.firstPass)
		{
			if(!game.firstPass)
			{
				gameBall.position.mulAdd(gameBall.direction, gameBall.speed);
			}

			for (int i=0; i<playerRacket.length; i++)
			{
				if(game.twoPlayer)
				{
					playerRacket[i].position.y = Gdx.graphics.getHeight() - (playerTouch.get(i).y + playerRacket[i].size.y/2.0f);
				}
				else
				{
					if(game.firstTouched == i)
					{
						playerRacket[i].position.y = Gdx.graphics.getHeight() - (playerTouch.get(i).y + playerRacket[i].size.y/2.0f);
					}
					else
					{
						if(game.firstPass)
						{
							playerRacket[i].position.y = Gdx.graphics.getHeight() - (playerTouch.get(i).y + playerRacket[i].size.y/2.0f);
						}
						else
						{
							playerRacket[i].position.y = computerRacketMove(gameBall, playerRacket[i]);
						}
					}
				}
			}

			boundaryCollisions(gameBall, playerRacket);

			Collision[] collision = new Collision[2];

			for (int i=0; i<collision.length; i++)
			{
				collision[i] = checkCollision(gameBall, playerRacket[i]);

				if(collision[i].detected)
				{
					collisionReposition(collision[i], gameBall);
					collisionRedirection(collision[i], gameBall, playerRacket[i]);

					if(!game.twoPlayer && game.firstTouched != i)
					{
						playerRacket[i].yCenter = (new Random()).nextInt((int) playerRacket[i].size.y);
					}

					if(game.soundOn) racketSound[i].play();
				}
			}

			int goaler = game.checkGoal(gameBall, playerRacket);

			if (goaler >= 0)
			{
				if (goaler == game.firstTouched)
				{
					int goalerScore = (goaler == 0) ? game.playerOneScore : game.playerTwoScore;

					gameBall.minSpeed = Math.min(gameBall.startSpeed + (float) goalerScore/50, gameBall.maxSpeed);

					gameBall.speed = Math.max(gameBall.minSpeed, gameBall.speed);
				}

				for (int i=0; i<playerRacket.length; i++)
				{
					if(!game.twoPlayer && game.firstTouched != i)
					{
						int scorePlayer = (game.firstTouched == 0) ? game.playerOneScore : game.playerTwoScore;
						int scoreComputer = (game.firstTouched == 0) ? game.playerTwoScore : game.playerOneScore;
						int deltaSpeed = Math.abs(scorePlayer - scoreComputer);
						int deltaSign = (scorePlayer < scoreComputer) ? -1 : 1;

						deltaSpeed *= deltaSign;

						playerRacket[i].curSpeed = playerRacket[i].minSpeed + deltaSpeed;

						playerRacket[i].curSpeed = Math.max(playerRacket[i].curSpeed, playerRacket[i].minSpeed);

						if (playerRacket[i].curSpeed >= playerRacket[i].maxSpeed)
						{
							gameBall.speed = gameBall.minSpeed + playerRacket[i].curSpeed - playerRacket[i].maxSpeed;
							gameBall.speed = Math.min(gameBall.speed, gameBall.maxSpeed);
						}

						break;
					}
				}
			}

			gameBall.sprite.setPosition(gameBall.position.x, gameBall.position.y);

			for (Racket racket : playerRacket) {
				racket.sprite.setPosition(racket.position.x, racket.position.y);
			}
		}

		if(game.firstPass) game.firstPass = false;

		batch.begin();

		String score1 = ((Integer) game.playerOneScore).toString();

		game.layout.setText
				(
						game.scoreFont,
						score1
				);

		game.scoreFont.draw
				(
						batch,
						score1,
						line.x - Math.max(game.layout.width*2, (float) Gdx.graphics.getWidth()/16) - game.layout.width/2,
						(float) Gdx.graphics.getHeight()/2 + game.layout.height/2
				);

		String score2 = ((Integer) game.playerTwoScore).toString();

		game.layout.setText
				(
						game.scoreFont,
						score2
				);

		game.scoreFont.draw
				(
						batch,
						score2,
						line.x + Math.max(game.layout.width*2, (float) Gdx.graphics.getWidth()/16) - game.layout.width/2,
						(float) Gdx.graphics.getHeight()/2 + game.layout.height/2
				);

		frame.sprite.draw(batch);

		line.sprite.draw(batch);

		gameBall.sprite.draw(batch);

		int i = 0, playerRacketLength = playerRacket.length;
		while (i < playerRacketLength) {
			Racket racket = playerRacket[i];
			racket.sprite.draw(batch);
			i++;
		}

		if(game.paused)
		{
			if(game.newGame)
			{
				game.layout.setText
						(
								game.scoreFont,
								game.startText
						);

				game.scoreFont.draw
						(
								batch,
								game.startText,
								(float) Gdx.graphics.getWidth()/2 - game.layout.width/2,
								(float) Gdx.graphics.getHeight()/2 + (float) Gdx.graphics.getHeight()/4
						);
			}
			else
			{
				switch(game.currentWindow)
				{
					case "MainMenu":
						game.drawPauseMenu();
						break;
					case "AboutWindow":
						game.drawAboutWindow();
						break;
				}
			}
		}

		batch.end();
	}

	@Override
	public void dispose()
	{
		batch.dispose();
		//font.dispose();
		game.scoreFont.dispose();
		game.pauseMenuBackground.texture.dispose();
		game.pauseMenuFrame.texture.dispose();
		game.pauseMenuFontUp.dispose();
		game.pauseMenuFontDown.dispose();
		game.aboutBackground.texture.dispose();
		game.aboutFrame.texture.dispose();
		game.aboutFont.dispose();
		gameBall.texture.dispose();
		for (Racket racket : playerRacket) {
			racket.texture.dispose();
		}
		line.texture.dispose();
		frame.texture.dispose();
		for (Sound sound : racketSound) {
			sound.dispose();
		}
		wallSound.dispose();
	}

	@Override
	public void resize(int width, int height)
	{
	}

	@Override
	public void pause()
	{
		game.paused = true;
	}

	@Override
	public void resume() {}

	@Override
	public boolean keyDown(int keycode) {
		return false; }

	@Override
	public boolean keyUp(int keycode) {
		return false; }

	@Override
	public boolean keyTyped(char character) {
		return false; }

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button)
	{
		int t;

		if (screenX < Gdx.graphics.getWidth()/2)
		{
			t=0;
		}
		else
		{
			t=1;
		}

		if(game.newGame)
		{
			game.newGame = false;
			game.paused = false;
			game.firstTouched = t;
		}
		else
		{
			if (game.paused)
			{
				for (int i=0; i < game.pauseMenuItem.length; i++)
				{
					if
					(
							Gdx.graphics.getWidth() - screenX >= game.pauseMenuItem[i].position.x &&
									Gdx.graphics.getWidth() - screenX <= game.pauseMenuItem[i].position.x + game.pauseMenuItem[i].size.x &&
									Gdx.graphics.getHeight() - screenY >= game.pauseMenuItem[i].position.y &&
									Gdx.graphics.getHeight() - screenY <= game.pauseMenuItem[i].position.y + game.pauseMenuItem[i].size.y
					)
					{
						game.pauseMenuItem[i].clicked = true;
					}
				}
			}
		}

		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button)
	{
		boolean clicked = false;
		boolean prefsClicked = false;

		if(game.paused)
		{
			for(int i=0; i<game.pauseMenuItem.length; i++)
			{
				if(game.pauseMenuItem[i].clicked)
				{
					clicked = true;
					break;
				}
			}

			if(clicked)
			{
				for (int i=0; i < game.pauseMenuItem.length; i++)
				{
					game.pauseMenuItem[i].clicked = false;

					if
					(
							Gdx.graphics.getWidth() - screenX >= game.pauseMenuItem[i].position.x &&
									Gdx.graphics.getWidth() - screenX <= game.pauseMenuItem[i].position.x + game.pauseMenuItem[i].size.x &&
									Gdx.graphics.getHeight() - screenY >= game.pauseMenuItem[i].position.y &&
									Gdx.graphics.getHeight() - screenY <= game.pauseMenuItem[i].position.y + game.pauseMenuItem[i].size.y
					)
					{
						game.layout.setText
								(
										game.pauseMenuFontDown,
										game.pauseMenuItem[i].text[game.pauseMenuItem[i].textIndex]
								);

						switch (game.pauseMenuItem[i].text[game.pauseMenuItem[i].textIndex])
						{
							case "BACK":
								game.paused = false;
								break;
							case "NEW GAME":
								create();
								break;
							case "ONE PLAYER":
								game.pauseMenuItem[i].textIndex = (game.pauseMenuItem[i].textIndex+1)%game.pauseMenuItem[i].text.length;
								game.pauseMenuItem[i].position.x = game.pauseMenuBackground.x + (game.pauseMenuBackground.width - game.layout.width)/2;
								game.twoPlayer = true;
								this.prefs.putBoolean("twoPlayer", true);
								prefsClicked = true;
								break;
							case "TWO PLAYER":
								game.pauseMenuItem[i].textIndex = (game.pauseMenuItem[i].textIndex+1)%game.pauseMenuItem[i].text.length;
								game.pauseMenuItem[i].position.x = game.pauseMenuBackground.x + (game.pauseMenuBackground.width - game.layout.width)/2;
								game.twoPlayer = false;
								this.prefs.putBoolean("twoPlayer", false);
								prefsClicked = true;
								break;
							case "SOUND OFF":
								game.pauseMenuItem[i].textIndex = (game.pauseMenuItem[i].textIndex+1)%game.pauseMenuItem[i].text.length;
								game.pauseMenuItem[i].position.x = game.pauseMenuBackground.x + (game.pauseMenuBackground.width - game.layout.width)/2;
								game.soundOn = true;
								this.prefs.putBoolean("soundOn", true);
								prefsClicked = true;
								break;
							case "SOUND ON":
								game.pauseMenuItem[i].textIndex = (game.pauseMenuItem[i].textIndex+1)%game.pauseMenuItem[i].text.length;
								game.pauseMenuItem[i].position.x = game.pauseMenuBackground.x + (game.pauseMenuBackground.width - game.layout.width)/2;
								game.soundOn = false;
								this.prefs.putBoolean("soundOn", false);
								prefsClicked = true;
								break;
							case "ABOUT":
								game.currentWindow = "AboutWindow";
								break;
							case "EXIT":
								Gdx.app.exit();
								break;
						}
					}
				}

				if (prefsClicked) this.prefs.flush();
			}
		}

		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer)
	{
		float deltaX;
		float deltaY;
		int i;

		if(!game.paused)
		{
			if (screenX < Gdx.graphics.getWidth()/2)
			{
				i=0;
			}
			else
			{
				i=1;
			}

			if (pointer < 2)
			{
				if (game.twoPlayer || game.firstTouched == i)
				{
					deltaX = screenX - playerTouch.get(pointer).x;
					deltaY = screenY - playerTouch.get(pointer).y;

					playerTouch.get(i).x = (float) screenX;
					playerTouch.get(i).y = (float) screenY;

					if (deltaX <= 0.0f)
					{
						if (deltaX == 0.0f)
						{
							playerRacket[i].direction.x = 0.0f;
						}
						else
						{
							playerRacket[i].direction.x = -1.0f;
						}
					}
					{
						playerRacket[i].direction.x = 1.0f;
					}

					if (deltaY <= 0.0f)
					{
						if (deltaY == 0.0f)
						{
							playerRacket[i].direction.x = 0.0f;
						}
						else
						{
							playerRacket[i].direction.y = -1.0f;
						}
					}
					{
						playerRacket[i].direction.y = 1.0f;
					}
				}
			}
		}

		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false; }

	@Override
	public boolean scrolled(float amountX, float amountY) {
		return false;
	}
}
